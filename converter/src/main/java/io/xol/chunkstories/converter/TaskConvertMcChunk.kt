//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.converter

import io.xol.chunkstories.api.converter.MinecraftBlocksTranslator
import io.xol.chunkstories.api.converter.mappings.NonTrivialMapper
import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.api.world.cell.FutureCell
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.converter.ConverterWorkers.ConverterWorkerThread
import io.xol.chunkstories.util.concurrency.CompoundFence
import io.xol.chunkstories.world.storage.ChunkHolderImplementation
import io.xol.enklume.MinecraftChunk
import io.xol.enklume.MinecraftRegion

class TaskConvertMcChunk(private val minecraftRegion: MinecraftRegion, private val minecraftChunk: MinecraftChunk?, private val chunkStoriesCurrentChunkX: Int, private val chunkStoriesCurrentChunkZ: Int, private val minecraftCurrentChunkXinsideRegion: Int, private val minecraftCuurrentChunkZinsideRegion: Int, private val minecraftRegionX: Int, private val minecraftRegionZ: Int, private val mappers: MinecraftBlocksTranslator) : Task() {

    override fun task(taskExecutor: TaskExecutor): Boolean {

        val thread = taskExecutor as ConverterWorkerThread
        val csWorld = thread.world()

        try {
            // Tries loading the Minecraft chunk
            if (minecraftChunk != null) {
                // If it succeed, we first require to load the corresponding chunkstories stuff
                val compoundFence = CompoundFence()

                // Then the chunks
                var y = 0
                while (y < OfflineWorldConverter.mcWorldHeight) {
                    val holder = csWorld.acquireChunkHolderWorldCoordinates(thread, chunkStoriesCurrentChunkX, y, chunkStoriesCurrentChunkZ) as ChunkHolderImplementation
                    compoundFence.add(holder.waitUntilStateIs(ChunkHolder.State.Available::class.java))

                    if (thread.aquiredChunkHolders.add(holder))
                        thread.chunksAcquired++
                    y += 32
                }

                // Wait for them to actually load
                compoundFence.traverse()
                for (x in 0..15)
                    for (z in 0..15)
                        for (y in 0 until OfflineWorldConverter.mcWorldHeight) {
                            // Translate each block
                            val mcId = minecraftChunk.getBlockID(x, y, z) and 0xFFF
                            val meta = (minecraftChunk.getBlockMeta(x, y, z) and 0xF).toByte()

                            // Ignore air blocks
                            if (mcId != 0) {
                                val mapper = this.mappers.getMapper(mcId, meta) ?: continue

                                if (mapper is NonTrivialMapper) {
                                    mapper.output(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, mcId, meta.toInt(), minecraftRegion,
                                            minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z)
                                } else {
                                    val future = FutureCell(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z,
                                            csWorld.content.voxels().air())

                                    // Directly set trivial blocks
                                    mapper.output(mcId, meta, future)
                                    if (!future.voxel!!.isAir())
                                        csWorld.pokeSimpleSilently(future)
                                }
                            }
                        }

            }
        } catch (e: Exception) {
            thread.converter().verbose(
                    "Issue with chunk " + minecraftCurrentChunkXinsideRegion + " " + minecraftCuurrentChunkZinsideRegion + " of region " + minecraftRegionX
                            + " " + minecraftRegionZ + ".")
            e.printStackTrace()
        }

        return true
    }

}
