package io.xol.chunkstories.graphics.vulkan.systems.world

import com.sun.org.apache.xpath.internal.operations.Bool
import io.xol.chunkstories.api.voxel.Voxel
import io.xol.chunkstories.api.voxel.VoxelFormat
import io.xol.chunkstories.api.voxel.VoxelSide
import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.world.chunk.ChunkRenderingData
import io.xol.chunkstories.world.chunk.CubicChunk
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

class VulkanChunkRenderData(val backend: VulkanGraphicsBackend, chunk: CubicChunk) : ChunkRenderingData(chunk) {
    val semaphore = Semaphore(1)

    private var lastBlock: Block? = null

    fun getLastBlock(): Block? {
        try {
            semaphore.acquireUninterruptibly()
            if (lastBlock == null)
                launchTask()

            return lastBlock?.apply { users.incrementAndGet() }
        } finally {
            semaphore.release()
        }
    }

    fun setLastBlock(block: Block) {
        try {
            semaphore.acquireUninterruptibly()
            if (lastBlock != null)
                lastBlock!!.doneWith()

            lastBlock = block
        } finally {
            semaphore.release()
        }
    }

    override fun destroy() {
        try {
            semaphore.acquireUninterruptibly()
            if (lastBlock != null)
                lastBlock!!.doneWith()
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

    class GenerateChunkDataTask(val backend: VulkanGraphicsBackend, val chunk: CubicChunk) : Task() {

        lateinit var rawChunkData: IntArray

        inline fun opaque(voxel: Voxel) = voxel.solid || voxel.name == "water"

        inline fun opaque(x2: Int, y2: Int, z2: Int): Boolean =
                if (x2 in 0..31 && y2 in 0..31 && z2 in 0..31) {
                    val data3 = rawChunkData[x2 * 32 * 32 + y2 * 32 + z2]
                    if(data3 != 0) {
                        val voxel2 = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data3))!!
                        opaque(voxel2)
                    } else false
                } else {
                    val data3 = chunk.world.peekRaw(x2 + chunk.chunkX * 32, y2 + chunk.chunkY * 32, z2 + chunk.chunkZ * 32)
                    if(data3 != 0) {
                        val voxel2 = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data3))!!
                        opaque(voxel2)
                    } else false
                }

        lateinit var cachedOpacity: BooleanArray

        inline fun cachedOpaque(x: Int, y: Int, z: Int) = cachedOpacity[(x + 1) * 34 * 34 + (y + 1) * 34 + (z + 1)]

        companion object {
            val threadLocalOpacity = object: ThreadLocal<BooleanArray>() {
                override fun initialValue() = BooleanArray(34 * 34 * 34)
            }
        }

        override fun task(taskExecutor: TaskExecutor?): Boolean {
            if (!chunk.holder().isChunkLoaded || chunk.holder().region.isUnloaded)
                return true

            val neighborsPresent = neighborsIndexes.count { (x, y, z) ->
                val neighbor = chunk.world.getChunk(chunk.chunkX + x, chunk.chunkY + y, chunk.chunkZ + z)
                (neighbor != null || (chunk.chunkY + y < 0) || (chunk.chunkY + y >= chunk.world.worldInfo.size.heightInChunks))
            }
            if (neighborsPresent < neighborsIndexes.size)
                return true

            val rng = Random(1)
            var count = 0
            val vertexBuffer: VulkanVertexBuffer?

            val chunkDataRef = chunk.chunkVoxelData
            if (chunk.isAirChunk || chunkDataRef == null) {
                vertexBuffer = null
            } else {
                rawChunkData = chunkDataRef

                // Cache the opacity so we don't waste time doing those checks over and over again
                //cachedOpacity = BooleanArray(34 * 34 * 34)
                //Arrays.fill(cachedOpacity)
                /*cachedOpacity = threadLocalOpacity.get()
                for (x in -1..32) {
                    for (y in -1..32) {
                        for (z in -1..32) {
                            cachedOpacity[(x + 1) * 34 * 34 + (y + 1) * 34 + (z + 1)] = true//opaque(x, y, z)
                        }
                    }
                }*/

                val buffer = memAlloc(65536 * 32)

                for (x in 0..31) {
                    for (y in 0..31) {
                        for (z in 0..31) {
                            val currentVoxelData = rawChunkData[x * 32 * 32 + y * 32 + z]
                            val currentVoxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(currentVoxelData))!!

                            if (opaque(currentVoxel)) {
                                if (opaque(x, y - 1, z) && opaque(x, y + 1, z) && opaque(x + 1, y, z) && opaque(x - 1, y, z) && opaque(x, y, z + 1) && opaque(x, y, z - 1))
                                    continue

                                buffer.putFloat(x.toFloat() + chunk.chunkX * 32f)
                                buffer.putFloat(y.toFloat() + chunk.chunkY * 32f)
                                buffer.putFloat(z.toFloat() + chunk.chunkZ * 32f)

                                val tex = currentVoxel.voxelTextures[VoxelSide.TOP.ordinal]//voxel?.getVoxelTexture(cell, VoxelSide.TOP)
                                val color = Vector4f(tex.color ?: Vector4f(1f, 0f, 0f, 1f))
                                if (color.w < 1.0f)
                                    color.mul(Vector4f(0f, 1f, 0.3f, 1.0f))
                                //color.mul(cell.sunlight / 15f)
                                color.mul(0.9f + rng.nextFloat() * 0.1f)

                                //val color = Vector4f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), 1f)
                                buffer.putFloat(color.x())
                                buffer.putFloat(color.y())
                                buffer.putFloat(color.z())
                                count++
                            }
                        }
                    }
                }

                buffer.flip()

                if (buffer.limit() > 0) {
                    vertexBuffer = VulkanVertexBuffer(backend, buffer.limit().toLong())
                    vertexBuffer.upload(buffer)
                } else
                    vertexBuffer = null

                memFree(buffer)
            }

            val generatedData = Block(chunk, vertexBuffer, count)
            (chunk.meshData as VulkanChunkRenderData).setLastBlock(generatedData)
            return true
        }
    }

    class Block(val chunk: CubicChunk, val vertexBuffer: VulkanVertexBuffer?, val count: Int) {
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