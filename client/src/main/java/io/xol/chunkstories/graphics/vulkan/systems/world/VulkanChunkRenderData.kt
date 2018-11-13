package io.xol.chunkstories.graphics.vulkan.systems.world

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.world.chunk.ChunkRenderingData
import io.xol.chunkstories.world.chunk.CubicChunk
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

class VulkanChunkRenderData(val backend: VulkanGraphicsBackend, chunk: CubicChunk) : ChunkRenderingData(chunk) {
    val semaphore = Semaphore(1)

    private var isDestroyed = false
    private var currentData: ChunkMeshInstance? = null

    fun getData(): ChunkMeshInstance? {
        try {
            semaphore.acquireUninterruptibly()
            if (currentData == null)
                launchTask()

            return currentData?.apply { users.incrementAndGet() }
        } finally {
            semaphore.release()
        }
    }

    fun acceptNewData(block: ChunkMeshInstance) {
        try {
            semaphore.acquireUninterruptibly()
            if (currentData != null)
                currentData!!.doneWith()

            if(isDestroyed)
                block.doneWith()
            else
                currentData = block
        } finally {
            semaphore.release()
        }
    }

    override fun destroy() {
        try {
            semaphore.acquireUninterruptibly()
            if (currentData != null)
                currentData!!.doneWith()

            currentData = null
            isDestroyed = true
        } finally {
            semaphore.release()
        }
    }

    var task: GenerateChunkDataTask? = null

    //TODO schedule this shit better
    override fun incrementPendingUpdates() {
        launchTask()
    }

    fun launchTask() {
        if ((task != null && !(task?.isCancelled == true || task?.isDone == true)))
            return

        task = GenerateChunkDataTask(backend, chunk)
        backend.window.client.tasks.scheduleTask(task)
    }

    companion object {
        val neighborsIndexes = generateNeighbors()

        fun generateNeighbors(): List<Triple<Int, Int, Int>> {
            val list = mutableListOf<Triple<Int, Int, Int>>()

            for (x in -1..1)
                for (y in -1..1)
                    for (z in -1..1)
                        list += Triple(x, y, z)

            return list.filterNot { (x, y, z) -> (x == 0 && y == 0 && z == 0) }
        }
    }

    class ChunkMeshInstance(val chunk: CubicChunk, val vertexBuffer: VulkanVertexBuffer?, val count: Int) {
        // User 0 isn't an user, it symbolizes the fact this block is still in use
        var users = AtomicInteger(1)

        fun doneWith() {
            if (users.decrementAndGet() == 0)
                destroy()
        }

        var once = false

        fun destroy() {
            if (once)
                System.exit(-88)

            //println("Destroying no longer needed data")
            vertexBuffer?.cleanup()
            once = true
        }
    }
}