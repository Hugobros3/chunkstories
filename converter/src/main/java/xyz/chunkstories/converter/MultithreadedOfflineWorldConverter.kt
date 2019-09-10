//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.converter

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.workers.Tasks
import xyz.chunkstories.api.world.WorldSize
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.util.concurrency.CompoundFence
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldTool
import xyz.chunkstories.world.chunk.ChunkHolderImplementation
import io.xol.enklume.MinecraftWorld
import io.xol.enklume.nbt.NBTInt
import java.io.File
import java.util.*

class MultithreadedOfflineWorldConverter
constructor(verboseMode: Boolean, mcFolder: File, csFolder: File, mcWorldName: String,
            csWorldName: String, size: WorldSize, minecraftOffsetX: Int, minecraftOffsetZ: Int, coreContentLocation: File,
            private val threadsCount: Int) : OfflineWorldConverter(verboseMode, mcFolder, csFolder, mcWorldName, csWorldName, size, minecraftOffsetX, minecraftOffsetZ, coreContentLocation) {

    private val workers: ConverterWorkers = ConverterWorkers(this, this.csWorld, threadsCount)

    override val tasks: Tasks
        get() = workers

    fun run() {
        val benchmarkingStart = System.currentTimeMillis()

        // Step one: copy the entire world data
        stepOneCopyWorldData(mcWorld, csWorld, minecraftOffsetX, minecraftOffsetZ)
        // Step two: make the summary data for chunk stories
        stepTwoCreateHeightmapData(csWorld)
        // Step three: redo the lightning of the entire map
        stepThreeSpreadLightning(csWorld)
        // Step four: fluff
        stetFourTidbits(mcWorld, csWorld)

        val timeTook = System.currentTimeMillis() - benchmarkingStart
        val timeTookSeconds = timeTook / 1000.0

        // Destroy the workers or it won't do shit
        this.workers.destroy()

        logger.info("Done converting $mcWorldName, took $timeTookSeconds seconds.")
    }

    protected fun stepOneCopyWorldData(mcWorld: MinecraftWorld, csWorld: WorldTool, minecraftOffsetX: Int,
                                       minecraftOffsetZ: Int) {
        csWorld.isLightningEnabled = false
        csWorld.isGenerationEnabled = false

        verbose("Entering step one: converting raw block data")

        // Prepares the loops
        val size = csWorld.worldInfo.size

        val mcRegionStartX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX)
        val mcRegionStartZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ)

        val mcRegionEndX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX + size.sizeInChunks * 32)
        val mcRegionEndZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ + size.sizeInChunks * 32)

        var minecraftChunksImported = 0
        val minecraftChunksToImport = (size.sizeInChunks * 32).toLong() * (size.sizeInChunks * 32).toLong() / (16 * 16)

        var completion = 0.0
        var lastPercentageShow = System.currentTimeMillis()

        try {
            // We do this minecraft region per minecraft region.
            for (minecraftRegionX in mcRegionStartX until mcRegionEndX) {
                for (minecraftRegionZ in mcRegionStartZ until mcRegionEndZ) {
                    // Load the culprit (There isn't any fancy world management, the getRegion()
                    // actually loads the entire region file)
                    val minecraftRegion = mcWorld.getRegion(minecraftRegionX, minecraftRegionZ)

                    val waitForTheBoys = CompoundFence()

                    // Iterate over each chunk within the minecraft region
                    // TODO Good candidate for task-ifying
                    for (minecraftCurrentChunkXinsideRegion in 0..31) {
                        for (minecraftCuurrentChunkZinsideRegion in 0..31) {
                            // Map minecraft chunk-space to chunk stories's
                            val chunkStoriesCurrentChunkX = (minecraftCurrentChunkXinsideRegion + minecraftRegionX * 32) * 16 - minecraftOffsetX
                            val chunkStoriesCurrentChunkZ = (minecraftCuurrentChunkZinsideRegion + minecraftRegionZ * 32) * 16 - minecraftOffsetZ

                            // Is it within our borders ?
                            if (chunkStoriesCurrentChunkX >= 0
                                    && chunkStoriesCurrentChunkX < csWorld.worldInfo.size.sizeInChunks * 32
                                    && chunkStoriesCurrentChunkZ >= 0
                                    && chunkStoriesCurrentChunkZ < csWorld.worldInfo.size.sizeInChunks * 32) {

                                if (minecraftRegion != null) {
                                    val minecraftChunk = minecraftRegion.getChunk(
                                            minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion)

                                    val task = TaskConvertMcChunk(minecraftRegion, minecraftChunk,
                                            chunkStoriesCurrentChunkX, chunkStoriesCurrentChunkZ,
                                            minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion,
                                            minecraftRegionX, minecraftRegionZ, mappers)
                                    workers.scheduleTask(task)
                                    waitForTheBoys.add(task)
                                }
                            }

                        }
                    }

                    waitForTheBoys.traverse()
                    workers.dropAll()

                    // Close region
                    minecraftRegion?.close()
                    System.gc()

                    // Display progress
                    minecraftChunksImported += 32 * 32
                    if (Math.floor(
                                    minecraftChunksImported.toDouble() / minecraftChunksToImport.toDouble() * 100) > completion) {
                        completion = Math
                                .floor(minecraftChunksImported.toDouble() / minecraftChunksToImport.toDouble() * 100)

                        if (completion >= 100.0 || System.currentTimeMillis() - lastPercentageShow > 5000) {
                            verbose("$completion% ... (${csWorld.regionsManager.countChunks()} chunks loaded ) using ${Runtime.getRuntime().freeMemory() / 1024 / 1024}/${Runtime.getRuntime().maxMemory() / 1024 / 1024}Mb ")
                            lastPercentageShow = System.currentTimeMillis()
                        }
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // csWorld.unloadUselessData();
    }

    protected fun stepTwoCreateHeightmapData(csWorld: WorldTool) {
        verbose("Entering step two: making heightmap data")

        val size = csWorld.worldInfo.size

        var done = 0
        val todo = size.sizeInChunks / 8 * (size.sizeInChunks / 8)

        var completion = 0.0
        var lastPercentageShow = System.currentTimeMillis()

        val wavesSize = this.threadsCount * 4
        var wave = 0

        val compoundFence = CompoundFence()
        for (regionX in 0 until size.sizeInChunks / 8) {
            for (regionZ in 0 until size.sizeInChunks / 8) {
                val task = TaskBuildHeightmap(regionX, regionZ, csWorld)
                workers.scheduleTask(task)
                compoundFence.traverse()

                if (wave < wavesSize) {
                    wave++
                } else {
                    compoundFence.traverse()
                    compoundFence.clear()

                    wave = 0
                    done += wavesSize

                    // Display progress...
                    if (Math.floor(done.toDouble() / todo.toDouble() * 100) > completion) {
                        completion = Math.floor(done.toDouble() / todo.toDouble() * 100)

                        if (completion >= 100.0 || System.currentTimeMillis() - lastPercentageShow > 5000) {
                            verbose("$completion% ... using ${Runtime.getRuntime().freeMemory() / 1024 / 1024}/${Runtime.getRuntime().maxMemory() / 1024 / 1024}Mb ")
                            lastPercentageShow = System.currentTimeMillis()
                        }
                    }

                    // Drop all unsued chunk data
                    workers.dropAll()
                }
            }
        }
        compoundFence.traverse()

        verbose("Saving unused chunk data...")
        // Drop all unsued chunk data
        workers.dropAll()
        // csWorld.unloadUselessData().traverse();
        verbose("Done.")
    }

    protected fun stepThreeSpreadLightning(csWorld: WorldTool) {
        verbose("Entering step three: spreading light")
        csWorld.isLightningEnabled = true

        val size = csWorld.worldInfo.size
        val maxHeightPossible = 256

        var done = 0
        val todo = size.sizeInChunks * size.sizeInChunks

        var completion = 0.0
        var lastPercentageShow = System.currentTimeMillis()

        val aquiredChunkHolders = HashSet<ChunkHolder>()
        val aquiredHeightmaps = HashSet<Heightmap>()

        var chunksacquired = 0
        val worldUser = this

        val waveSize = this.threadsCount * 32
        var wave = 0

        val waveFence = CompoundFence()

        for (chunkX in 0 until size.sizeInChunks) {
            for (chunkZ in 0 until size.sizeInChunks) {
                wave++

                val compoundFence = CompoundFence()

                //val heightmap = csWorld.regionsSummariesHolder.acquireHeightmapChunkCoordinates(worldUser, chunkX, chunkZ)
                //aquiredHeightmaps.add(heightmap)

                /*when(val state = heightmap.state) {
                    is Heightmap.State.Loading -> compoundFence.add(state.fence)
                    !is Heightmap.State.Available -> throw Exception("Heightmap state isn't available or loading, unexpected behavior met")
                }*/
                //compoundFence.add(heightmap.waitUntilStateIs(Heightmap.State.Available::class.java))

                // Loads 3x3 arround relevant chunks
                for (i in -1..1) {
                    for (j in -1..1) {
                        for (chunkY in 0..maxHeightPossible / 32) {
                            val chunkHolder = csWorld.chunksManager.acquireChunkHolder(worldUser, chunkX + i, chunkY, chunkZ + j) as ChunkHolderImplementation
                            /*when(val state = chunkHolder.state) {
                                is ChunkHolder.State.Loading -> compoundFence.add(state.fence)
                                !is ChunkHolder.State.Available -> throw Exception("ChunkHolder state isn't available or loading, unexpected behavior met")
                            }*/
                            compoundFence.add(chunkHolder.waitUntilStateIs(ChunkHolder.State.Available::class.java))

                            if (aquiredChunkHolders.add(chunkHolder))
                                chunksacquired++

                        }
                    }
                }

                assert(chunksacquired == aquiredChunkHolders.size)

                // Wait for everything to actually load
                compoundFence.traverse()

                // Spreads lightning, from top to botton
                for (chunkY in maxHeightPossible / 32 downTo 0) {
                    val chunk = csWorld.chunksManager.getChunk(chunkX, chunkY, chunkZ)
                    val fence = chunk!!.lightBaker.requestUpdateAndGetFence()
                    waveFence.add(fence)
                }

                if (wave >= waveSize) {
                    waveFence.traverse()
                    waveFence.clear()

                    while (true) {
                        if (workers.size() > 0) {
                            // we actually wait for the workers to chew through all their tasks
                            // hopefully nothing cocks about in the lightning code and spawns
                            // endless tasks
                            try {
                                Thread.sleep(50L)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        } else
                            break
                    }

                    wave = 0

                    // Show progress
                    done += waveSize
                    if (Math.floor(done.toDouble() / todo.toDouble() * 100) > completion) {
                        completion = Math.floor(done.toDouble() / todo.toDouble() * 100)

                        if (completion >= 100.0 || System.currentTimeMillis() - lastPercentageShow > 5000) {
                            verbose(completion.toString() + "% ... using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/"
                                    + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb ")
                            lastPercentageShow = System.currentTimeMillis()
                        }
                    }

                    if (aquiredChunkHolders.size > targetChunksToKeepInRam) {
                        // Save world
                        // verbose("More than "+targetChunksToKeepInRam+" chunks already in memory,
                        // saving and unloading before continuing");

                        // csWorld.saveEverything();
                        // for(Region region : registeredCS_Regions)
                        // region.unregisterUser(user);

                        for (holder in aquiredChunkHolders) {
                            holder.unregisterUser(worldUser)
                            chunksacquired--
                        }

                        aquiredHeightmaps.forEach { it.unregisterUser(worldUser) }

                        aquiredHeightmaps.clear()
                        aquiredChunkHolders.clear()
                    }
                }
            }
        }

        waveFence.traverse()
        wave = 0

        // Terminate
        for (holder in aquiredChunkHolders) {
            holder.unregisterUser(worldUser)
            chunksacquired--
        }

        for (heightmap in aquiredHeightmaps)
            heightmap.unregisterUser(worldUser)

        aquiredHeightmaps.clear()
        aquiredChunkHolders.clear()
    }

    protected fun stetFourTidbits(mcWorld: MinecraftWorld, csWorld: WorldImplementation) {
        verbose("Entering step four: tidbits")

        val spawnX = (mcWorld.levelDotDat.root.getTag("Data.SpawnX") as NBTInt).getData()
        val spawnY = (mcWorld.levelDotDat.root.getTag("Data.SpawnY") as NBTInt).getData()
        val spawnZ = (mcWorld.levelDotDat.root.getTag("Data.SpawnZ") as NBTInt).getData()

        csWorld.defaultSpawnLocation = Location(csWorld, spawnX.toDouble(), spawnY.toDouble(), spawnZ.toDouble())
        csWorld.saveEverything().traverse()

        csWorld.destroy()
    }

}
