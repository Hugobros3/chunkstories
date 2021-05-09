//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.ingame

import com.carrotsearch.hppc.IntHashSet
import xyz.chunkstories.api.exceptions.net.IllegalPacketException
import xyz.chunkstories.net.packets.PacketWorldUser
import xyz.chunkstories.net.packets.PacketWorldUser.Tag
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.api.world.region.Region
import xyz.chunkstories.util.concurrency.CompoundFence
import xyz.chunkstories.world.heightmap.HeightmapImplementation
import xyz.chunkstories.world.chunk.ChunkHolderImplementation
import xyz.chunkstories.world.region.RegionImplementation
import xyz.chunkstories.api.math.MathUtils.clamp
import xyz.chunkstories.api.math.MathUtils.mod_dist
import xyz.chunkstories.api.world.WorldUser
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.player.PlayerState
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.api.world.WorldSub
import xyz.chunkstories.client.InternalClientOptions

class LocalClientLoadingAgent(val ingame: IngameClientImplementation) {
    val user = object : WorldUser {}

    val aquiredChunkHoldersMask = IntHashSet()
    val acquiredChunkHolders = HashSet<ChunkHolder>()

    val acquiredHeightmapsMask = IntHashSet()
    val acquiredHeightmaps = HashSet<Heightmap>()

    private val lock = ReentrantLock()

    fun updateUsedWorldBits() {
        val position = when (val state = ingame.player.state) {
            PlayerState.None -> return
            is PlayerState.Ingame -> state.entity.location
            is PlayerState.Spectating -> state.location
        }

        val ipos = position.toVec3i()

        val cx = ipos.x / 32
        val cy = ipos.y / 32
        val cz = ipos.z / 32

        val chunksViewDistance = ingame.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val chunksViewDistanceHeight = 6

        updateUsedWorldBits(cx, cy, cz, chunksViewDistance, chunksViewDistanceHeight)
    }

    fun updateUsedWorldBits(cameraChunkX: Int, cameraChunkY: Int, cameraChunkZ: Int, chunkViewRange: Int, verticalChunkViewRange: Int) {
        val world = ingame.world

        try {
            lock.lock()

            val squareSizeInChunks = world.properties.size.sizeInChunks
            val worldInfo = world.properties
            val size = worldInfo.size
            val sizeInRegions = (world.properties.size.sizeInChunks / 8)
            val summaryDistance = 32//(int) (world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 24);

            for (chunkX in cameraChunkX - chunkViewRange - 1..cameraChunkX + chunkViewRange + 1) {
                for (chunkZ in cameraChunkZ - chunkViewRange - 1..cameraChunkZ + chunkViewRange + 1)
                    for (chunkY in cameraChunkY - verticalChunkViewRange - 1..cameraChunkY + verticalChunkViewRange + 1) {
                        if (chunkY < 0 || chunkY >= world.properties.size.heightInChunks)
                            continue

                        val filteredChunkX = chunkX and size.maskForChunksCoordinates
                        val filteredChunkY = clamp(chunkY, 0, size.sizeInChunks - 1)
                        val filteredChunkZ = chunkZ and size.maskForChunksCoordinates

                        val summed = (((filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates) or filteredChunkY) shl size.bitlengthOfHorizontalChunksCoordinates) or filteredChunkZ

                        if (aquiredChunkHoldersMask.contains(summed)) {
                            //val chunkHolder = usedChunks.find { it.chunkX == filteredChunkX && it.chunkY == filteredChunkY && it.chunkZ == filteredChunkZ }
                            //println("$chunkHolder + ${chunkHolder?.region}")
                            continue
                        }

                        val holder = world.chunksManager.acquireChunkHolder(user, chunkX, chunkY, chunkZ)!!

                        assert(holder.region.state !is Region.State.Zombie)

                        acquiredChunkHolders.add(holder)
                        aquiredChunkHoldersMask.add(summed)

                        if (world is WorldSub) {
                            world.pushPacket(PacketWorldUser.registerChunkPacket(world, filteredChunkX, filteredChunkY, filteredChunkZ))
                        }
                    }
            }

            // Unsubscribe for far ones
            val i = acquiredChunkHolders.iterator()
            while (i.hasNext()) {
                val holder = i.next()
                if (mod_dist(holder.chunkX, cameraChunkX, squareSizeInChunks) > chunkViewRange + 1
                        || mod_dist(holder.chunkZ, cameraChunkZ, squareSizeInChunks) > chunkViewRange + 1
                        || Math.abs(holder.chunkY - cameraChunkY) > verticalChunkViewRange + 1) {

                    val filteredChunkX = holder.chunkX and size.maskForChunksCoordinates
                    val filteredChunkY = clamp(holder.chunkY, 0, 31)
                    val filteredChunkZ = holder.chunkZ and size.maskForChunksCoordinates

                    val summed = (((filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates) or filteredChunkY) shl size.bitlengthOfHorizontalChunksCoordinates) or filteredChunkZ

                    aquiredChunkHoldersMask.remove(summed)

                    i.remove()
                    holder.unregisterUser(user)

                    if (world is WorldSub) {
                        world.pushPacket(PacketWorldUser.unregisterChunkPacket(world, filteredChunkX, filteredChunkY, filteredChunkZ))
                    }
                }
            }

            for (chunkX in cameraChunkX - summaryDistance until cameraChunkX + summaryDistance)
                for (chunkZ in cameraChunkZ - summaryDistance until cameraChunkZ + summaryDistance) {
                    if (chunkX % 8 == 0 && chunkZ % 8 == 0) {
                        var regionX: Int = (chunkX / 8) % sizeInRegions
                        var regionZ: Int = (chunkZ / 8) % sizeInRegions

                        // sanitizing the regions is needed here
                        if(regionX < 0) {
                            regionX += sizeInRegions
                        }
                        if(regionZ < 0) {
                            regionZ += sizeInRegions
                        }

                        val key = regionX * sizeInRegions + regionZ

                        if (!acquiredHeightmapsMask.contains(key)) {
                            acquiredHeightmapsMask.add(key)

                            val regionSummary = world.heightmapsManager.acquireHeightmap(user, regionX, regionZ)
                            acquiredHeightmaps.add(regionSummary)
                            if (world is WorldSub) {
                                world.pushPacket(PacketWorldUser.registerSummary(world, regionX, regionZ))
                            }
                        }
                    }
                }

            val cameraRegionX = cameraChunkX / 8
            val cameraRegionZ = cameraChunkZ / 8

            val distInRegions = summaryDistance / 8

            // And we unload the ones we no longer need
            val iterator = acquiredHeightmaps.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val regionX = entry.regionX
                val regionZ = entry.regionZ

                val key = regionX * sizeInRegions + regionZ

                val dx = mod_dist(cameraRegionX, regionX, sizeInRegions)
                val dz = mod_dist(cameraRegionZ, regionZ, sizeInRegions)
                if (dx > distInRegions + 1 || dz > distInRegions + 1) {
                    entry.unregisterUser(user)
                    iterator.remove()
                    acquiredHeightmapsMask.remove(key)

                    if (world is WorldSub) {
                        world.pushPacket(PacketWorldUser.unregisterSummary(world, regionX, regionZ))
                    }
                }
            }

        } finally {
            lock.unlock()
        }
    }

    fun handleServerResponse(packet: PacketWorldUser) {
        val world = ingame.world
        try {
            lock.lock()
            // The server refused to register us to this chunk. We gracefully accept.
            if (packet.tag == Tag.UNREGISTER_CHUNK) {
                val holder = world.regionsManager.getRegionChunkCoordinates(packet.x, packet.y, packet.z)!!
                        .getChunkHolder(packet.x, packet.y, packet.z) ?: return

                // Apparently we already figured we didn't need this anyway

                val worldInfo = world.properties
                val size = worldInfo.size

                val filteredChunkX = holder.chunkX and size.maskForChunksCoordinates
                val filteredChunkY = clamp(holder.chunkY, 0, 31)
                val filteredChunkZ = holder.chunkZ and size.maskForChunksCoordinates

                val summed = filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates or filteredChunkY shl size.bitlengthOfHorizontalChunksCoordinates or filteredChunkZ

                // We remove it from our list
                aquiredChunkHoldersMask.remove(summed)
                acquiredChunkHolders.remove(holder)

                // And we unsub.
                holder.unregisterUser(user)

                // This is the same but for region summaries
            } else if (packet.tag == Tag.UNREGISTER_SUMMARY) {
                val regionSummary = world.heightmapsManager.getHeightmap(packet.x, packet.z) ?: return

                acquiredHeightmaps.remove(regionSummary)
                regionSummary.unregisterUser(user)

            } else
            // We only expect UNREGISTER packets from the server !
                throw IllegalPacketException(packet)
        } finally {
            lock.unlock()
        }
    }

    fun unloadEverything(andWait: Boolean) {
        val compoundFence = CompoundFence()

        logger.debug("Unsubscribing to everything and waiting on everything to get back to normal...")

        val regions = mutableSetOf<RegionImplementation>()

        for(chunkHolder in acquiredChunkHolders) {
            chunkHolder.unregisterUser(user)
            (chunkHolder as ChunkHolderImplementation).waitUntilStateIs(ChunkHolder.State.Unloaded::class.java).traverse()
            regions.add(chunkHolder.region)
        }

        for(region in regions)
            region.waitUntilStateIs(Region.State.Zombie::class.java)

        for(heightmap in acquiredHeightmaps) {
            heightmap.unregisterUser(user)
            (heightmap as HeightmapImplementation).waitUntilStateIs(Heightmap.State.Zombie::class.java).traverse()
        }

        logger.debug("Additional wait for heightmaps that were transitively loaded")

        for(heightmap in ingame.world.heightmapsManager.all()) {
            heightmap.waitUntilStateIs(Heightmap.State.Zombie::class.java).traverse()
        }

        logger.debug("Done waiting! World should be saved now :)")

        if(andWait)
            compoundFence.traverse()
    }

    companion object {
        val logger = LoggerFactory.getLogger("world.io")
    }
}
