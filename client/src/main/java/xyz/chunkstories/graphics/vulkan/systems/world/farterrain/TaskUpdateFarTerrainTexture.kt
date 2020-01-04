package xyz.chunkstories.graphics.vulkan.systems.world.farterrain

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkImageMemoryBarrier
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.workers.Task
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import xyz.chunkstories.graphics.vulkan.util.createFence
import xyz.chunkstories.graphics.vulkan.util.waitFence
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.heightmap.HeightmapImplementation
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

data class FarTerrainTextureManager(val backend: VulkanGraphicsBackend, var baseX: Int, var baseZ: Int, val size: Int) : Cleanable {
    val heightTexture = VulkanTexture2D(backend, TextureFormat.RED_16I, 4096, 4096, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)

    val state = IntArray(size * size)
    var representingX = IntArray(size * size) { -1 }
        private set
    var representingZ = IntArray(size * size) { -1 }
        private set

    private var representingX_ = IntArray(size * size) { -1 }
    private var representingZ_ = IntArray(size * size) { -1 }

    val dataLock = ReentrantLock()
    val taskLock = ReentrantLock()
    var task: TaskUpdateFarTerrainTexture? = null
    var request: RequestUpdate? = null

    data class RequestUpdate(val newX: Int, val newZ: Int, val world: WorldClientCommon)

    fun requestUpdate(newX: Int, newZ: Int, world: WorldClientCommon) {
        taskLock.lock()
        if (task == null) {
            world.client.tasks.scheduleTask(TaskUpdateFarTerrainTexture(newX, newZ, world))
        } else {
            request = RequestUpdate(newX, newZ, world)
        }
        taskLock.unlock()
    }

    init {
        heightTexture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    }

    inner class TaskUpdateFarTerrainTexture(val newX: Int, val newZ: Int, val world: WorldClientCommon) : Task() {
        private inner class HeightmapTemp(val destX: Int, val destZ: Int, val heightmap: HeightmapImplementation) {
            lateinit var buffer: ByteBuffer
        }

        override fun task(taskExecutor: TaskExecutor): Boolean {
            stackPush()

            val reqs = arrayListOf<HeightmapTemp>()
            val worldSizeInRegions = world.sizeInChunks / 8
            for (_rx in newX until newX + size) {
                for (_rz in newZ until newZ + size) {
                    // real regions coords are like so
                    val rx = _rx % worldSizeInRegions
                    val rz = _rz % worldSizeInRegions

                    // TOROIDAL addressing
                    val inArrayX = rx % size
                    val inArrayZ = rz % size
                    val index = inArrayX * size + inArrayZ

                    val heightmap = world.heightmapsManager.getHeightmap(rx, rz)
                    if (heightmap != null && heightmap.state is Heightmap.State.Available) {
                        val oldRepresentingX = representingX[index]
                        val oldRepresentingZ = representingZ[index]

                        if (oldRepresentingX != rx || oldRepresentingZ != rz) {
                            val req = HeightmapTemp(inArrayX * 256, inArrayZ * 256, heightmap)
                            req.buffer = memAlloc(256 * 256 * 2)
                            for (z in 0 until 256) {
                                for (x in 0 until 256) {
                                    req.buffer.putShort(heightmap.getHeight(x, z).toShort())
                                    //req.buffer.putShort((256 * Math.random()).toShort())
                                }
                            }
                            req.buffer.flip()
                            reqs.add(req)
                        }
                        representingX_[index] = rx
                        representingZ_[index] = rz
                    } else {
                        representingX_[index] = -1
                        representingZ_[index] = -1
                    }
                }
            }

            if (reqs.size > 0) {
                // start building cmdbuffer
                val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
                val commandBuffer = operationsPool.startCommandBuffer()

                preBarrier(commandBuffer)

                val scratchVkBuffer = VulkanBuffer(backend, reqs.size * 256 * 256 * 2L, VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT, MemoryUsagePattern.STAGING)

                val copies = VkBufferImageCopy.callocStack(16 * 16)
                var copiesCount = 0
                for (req in reqs) {
                    scratchVkBuffer.upload(req.buffer, copiesCount * 256 * 256 * 2L, 256 * 256 * 2L)
                    copies[copiesCount].apply {
                        bufferOffset(copiesCount * 256 * 256 * 2L)

                        // tightly packed
                        bufferRowLength(0)
                        bufferImageHeight(0)

                        imageSubresource().apply {
                            aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            mipLevel(0)
                            baseArrayLayer(0)
                            layerCount(1)
                        }

                        imageOffset().apply {
                            x(req.destX)
                            y(req.destZ)
                            z(0)
                        }

                        imageExtent().apply {
                            width(256)
                            height(256)
                            depth(1)
                        }
                    }
                    copiesCount++
                }
                copies.position(0)
                copies.limit(copiesCount)

                vkCmdCopyBufferToImage(commandBuffer, scratchVkBuffer.handle, heightTexture.imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copies)

                postBarrier(commandBuffer)
                vkEndCommandBuffer(commandBuffer)

                val fence = backend.createFence(false)

                // lock everything before executing cmd buffer!
                dataLock.lock()
                operationsPool.submitCmdBuffer(commandBuffer, backend.logicalDevice.graphicsQueue, fence)
                backend.waitFence(fence)

                // swap "representing" arrays!
                val trx = representingX
                representingX = representingX_
                representingX_ = trx
                val trz = representingZ
                representingZ = representingZ_
                representingZ_ = trz
                dataLock.unlock()

                // Cleanup everything
                vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)
                scratchVkBuffer.cleanup()
                reqs.forEach {
                    memFree(it.buffer)
                }
                stackPop()
            }

            taskLock.lock()
            task = null
            val lastRequest = request
            if (lastRequest != null) {
                world.client.tasks.scheduleTask(TaskUpdateFarTerrainTexture(lastRequest.newX, lastRequest.newZ, lastRequest.world))
                request = null
            }
            taskLock.unlock()

            return true
        }

        private fun preBarrier(commandBuffer: VkCommandBuffer) {
            val preUpdateBarrier = VkImageMemoryBarrier.callocStack(1).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                oldLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)

                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                image(heightTexture.imageHandle)

                subresourceRange().apply {
                    aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    baseMipLevel(0)
                    levelCount(1)
                    baseArrayLayer(0)
                    layerCount(1)
                }

                srcAccessMask(0)
                dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, preUpdateBarrier)
        }

        private fun postBarrier(commandBuffer: VkCommandBuffer) {
            val postUpdateBarrier = VkImageMemoryBarrier.callocStack(1).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                image(heightTexture.imageHandle)

                subresourceRange().apply {
                    aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    baseMipLevel(0)
                    levelCount(1)
                    baseArrayLayer(0)
                    layerCount(1)
                }

                srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, postUpdateBarrier)
        }
    }

    override fun cleanup() {
        heightTexture.cleanup()
    }
}