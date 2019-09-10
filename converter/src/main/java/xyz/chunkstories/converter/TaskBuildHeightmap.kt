//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.converter

import xyz.chunkstories.api.workers.Task
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.converter.ConverterWorkers.ConverterWorkerThread
import xyz.chunkstories.util.concurrency.CompoundFence
import xyz.chunkstories.world.WorldTool
import xyz.chunkstories.world.chunk.ChunkHolderImplementation

class TaskBuildHeightmap(private val regionX: Int, private val regionZ: Int, private val csWorld: WorldTool) : Task() {

    override fun task(taskExecutor: TaskExecutor): Boolean {
        val thread = taskExecutor as ConverterWorkerThread

        // We wait on a bunch of stuff to load everytime
        val compoundFence = CompoundFence()

        val heightmap = csWorld.heightmapsManager.acquireHeightmap(thread, regionX, regionZ)
        compoundFence.add(heightmap.waitUntilStateIs(Heightmap.State.Available::class.java))

        val heightInChunks = OfflineWorldConverter.mcWorldHeight / 32
        val holders = arrayOfNulls<ChunkHolder>(8 * 8 * heightInChunks)

        // acquires the chunks we want to make the summaries of.
        for (innerCX in 0..7)
            for (innerCZ in 0..7)
                for (chunkY in 0 until heightInChunks) {
                    val holder = csWorld.chunksManager.acquireChunkHolder(thread, regionX * 8 + innerCX, chunkY, regionZ * 8 + innerCZ) as ChunkHolderImplementation
                    holders[(innerCX * 8 + chunkY) * heightInChunks + innerCZ] = holder
                    compoundFence.add(holder.waitUntilStateIs(ChunkHolder.State.Available::class.java))

                    if (thread.aquiredChunkHolders.add(holder))
                        thread.chunksAcquired++
                }

        // Wait until all of that crap loads
        compoundFence.traverse()

        // Descend from top
        for (i in 0..255)
            for (j in 0..255) {
                for (h in OfflineWorldConverter.mcWorldHeight - 1 downTo 0) {
                    val cx = i / 32
                    val cy = h / 32
                    val cz = j / 32
                    val data = holders[(cx * 8 + cy) * heightInChunks + cz]!!.chunk!!.peek(i % 32, h % 32, j % 32)
                    if (!data.voxel!!.isAir()) {
                        val vox = data.voxel
                        if (vox!!.solid || vox.name == "water") {
                            heightmap.setTopCell(data)
                            break
                        }
                    }
                }
            }


        // We don't need the summary anymore
        heightmap.unregisterUser(thread)
        heightmap.waitUntilStateIs(Heightmap.State.Zombie::class.java)

        return true
    }

}
