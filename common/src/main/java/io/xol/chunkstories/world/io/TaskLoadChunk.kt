//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io

import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.world.storage.ChunkHolderImplementation
import io.xol.chunkstories.world.chunk.CompressedData
import io.xol.chunkstories.world.chunk.CubicChunk

class TaskLoadChunk(internal var chunkSlot: ChunkHolderImplementation) : Task() {

    public override fun task(taskExecutor: TaskExecutor): Boolean {
        val compressedData = chunkSlot.compressedData!!
        val chunk = CubicChunk(chunkSlot, chunkSlot.chunkX, chunkSlot.chunkY, chunkSlot.chunkZ, compressedData)
        chunkSlot.eventLoadFinishes(chunk)
        return true
    }

    override fun toString(): String {
        return "[TaskLoadChunk $chunkSlot]"
    }
}