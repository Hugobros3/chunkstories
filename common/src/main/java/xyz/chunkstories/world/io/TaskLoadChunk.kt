//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.io

import xyz.chunkstories.api.workers.Task
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.world.storage.ChunkHolderImplementation
import xyz.chunkstories.world.chunk.CompressedData
import xyz.chunkstories.world.chunk.CubicChunk

class TaskLoadChunk(internal var chunkSlot: ChunkHolderImplementation) : IOTask() {

    public override fun task(taskExecutor: TaskExecutor): Boolean {
        val compressedData = chunkSlot.compressedData
        val chunk = CubicChunk(chunkSlot, chunkSlot.chunkX, chunkSlot.chunkY, chunkSlot.chunkZ, compressedData)
        chunkSlot.eventLoadFinishes(chunk)
        return true
    }

    override fun toString(): String {
        return "[TaskLoadChunk $chunkSlot]"
    }
}