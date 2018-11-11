package io.xol.chunkstories.graphics.vulkan.systems.world

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

    private var lastBlock : Block? = null

    fun getLastBlock() : Block? {
        try {
            semaphore.acquireUninterruptibly()
            if(lastBlock == null)
                launchTask()

            return lastBlock?.apply { users.incrementAndGet() }
        } finally {
            semaphore.release()
        }
    }

    fun setLastBlock(block: Block) {
        try {
            semaphore.acquireUninterruptibly()
            if(lastBlock != null)
                lastBlock!!.doneWith()

            lastBlock = block
        } finally {
            semaphore.release()
        }
    }

    override fun destroy() {
        try {
            semaphore.acquireUninterruptibly()
            if(lastBlock != null)
                lastBlock!!.doneWith()
        } finally {
            semaphore.release()
        }
    }

    var task : GenerateChunkDataTask? = null

    fun launchTask() {
        if(task != null)
            return

        task = GenerateChunkDataTask(backend, chunk)
        backend.window.client.tasks.scheduleTask(task)
    }

    class GenerateChunkDataTask(val backend: VulkanGraphicsBackend, val chunk: CubicChunk) : Task() {
        override fun task(taskExecutor: TaskExecutor?): Boolean {
            val generatedData = Block(backend, chunk)
            (chunk.meshData as VulkanChunkRenderData).setLastBlock(generatedData)
            return true
        }
    }

    class Block(backend: VulkanGraphicsBackend, chunk: CubicChunk) {
        // User 0 isn't an user, it symbolizes the fact this block is still in use
        var users = AtomicInteger(1)

        var count: Int = 0
        val vertexBuffer: VulkanVertexBuffer?

        init {
            val rng = Random(1)

            if(chunk.isAirChunk) {
                vertexBuffer = null
            } else {
                val buffer = memAlloc(65536 * 32)

                val data = chunk.chunkVoxelData
                for(x in 0..31) {
                    for(y in 0..31) {
                        for(z in 0..31) {

                            val data2 = data[x * 32 * 32 + y * 32 + z]
                            val voxel = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data2))!!

                            fun opaque(voxel: Voxel) = voxel.solid || voxel.name == "water"

                            fun check(x2: Int, y2: Int, z2: Int) : Boolean {
                                if(x2 in 0..31 && y2 in 0..31 && z2 in 0..31) {
                                    val data3 = data[x2 * 32 * 32 + y2 * 32 + z2]
                                    val voxel2 = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data3))!!
                                    return opaque(voxel2)
                                } else {
                                    val data3 = chunk.world.peekRaw(x2 + chunk.chunkX * 32, y2 + chunk.chunkY * 32, z2 + chunk.chunkZ * 32)
                                    val voxel2 = chunk.world.contentTranslator.getVoxelForId(VoxelFormat.id(data3))!!
                                    return opaque(voxel2)
                                }
                            }

                            if(opaque(voxel)) {
                                if(check(x, y - 1, z) && check(x, y + 1, z) && check(x + 1, y, z) && check(x - 1, y, z) && check(x, y, z + 1) && check(x, y, z - 1))
                                    continue

                                buffer.putFloat(x.toFloat() + chunk.chunkX * 32f)
                                buffer.putFloat(y.toFloat() + chunk.chunkY * 32f)
                                buffer.putFloat(z.toFloat() + chunk.chunkZ * 32f)

                                val tex = voxel.voxelTextures[VoxelSide.TOP.ordinal]//voxel?.getVoxelTexture(cell, VoxelSide.TOP)
                                val color = Vector4f(tex.color ?: Vector4f(1f, 0f, 0f, 1f))
                                if(color.w < 1.0f)
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

                if(buffer.limit() > 0) {
                    vertexBuffer = VulkanVertexBuffer(backend, buffer.limit().toLong())
                    vertexBuffer.upload(buffer)
                } else
                    vertexBuffer = null

                memFree(buffer)
            }
        }

        fun doneWith() {
            if(users.decrementAndGet() == 0)
                destroy()
        }

        var once = false

        fun destroy() {
            if(once)
                System.exit(-88)

            println("Destroying no longer needed data")
            vertexBuffer?.cleanup()
            once = true
        }
    }
}