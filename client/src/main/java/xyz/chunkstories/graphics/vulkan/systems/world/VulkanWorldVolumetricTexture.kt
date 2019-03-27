package xyz.chunkstories.graphics.vulkan.systems.world

import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector4f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkImageMemoryBarrier
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.api.voxel.VoxelFormat
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture3D
import xyz.chunkstories.graphics.vulkan.util.createFence
import xyz.chunkstories.graphics.vulkan.util.waitFence
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.chunk.CubicChunk
import java.nio.ByteBuffer

class VulkanWorldVolumetricTexture(val backend: VulkanGraphicsBackend, val world: WorldClientCommon) : Cleanable {
    val size = 128
    val texture = VulkanTexture3D(backend, TextureFormat.RGBA_8, size, size, size, VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)

    val chunksSidesCount = size / 32
    val singleChunkSizeInRam = 32 * 32 * 32 * 4
    val scratchByteBuffer = memAlloc(chunksSidesCount * chunksSidesCount * chunksSidesCount * singleChunkSizeInRam)

    val info = VolumetricTextureMetadata()

    val chunksCache = arrayOfNulls<Chunk>(chunksSidesCount * chunksSidesCount * chunksSidesCount)
    val revisionCache = LongArray(chunksCache.size)

    init {
        texture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    }

    //val lastPos = Vector3i(0)

    fun updateArround(position: Vector3dc) {
        val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
        val commandBuffer = operationsPool.createOneUseCB()

        info.noise = (info.noise + 1) % 256
        //println(info.noise)

        stackPush().use {
            scratchByteBuffer.clear()

            val positioni = position.toVec3i()
            val chunkPositionX = (positioni.x + 16) / 32
            val chunkPositionY = (positioni.y + 16) / 32
            val chunkPositionZ = (positioni.z + 16) / 32

            val chunkStartX = chunkPositionX - chunksSidesCount / 2
            val chunkStartY = chunkPositionY - chunksSidesCount / 2
            val chunkStartZ = chunkPositionZ - chunksSidesCount / 2

            info.baseChunkPos.x = chunkStartX
            info.baseChunkPos.y = chunkStartY
            info.baseChunkPos.z = chunkStartZ

            /*if (info.baseChunkPos == lastPos)
                return

            lastPos.set(info.baseChunkPos)*/

            info.size = size

            val copies = VkBufferImageCopy.callocStack(chunksSidesCount * chunksSidesCount * chunksSidesCount)
            var copiesCount = 0
            for (x in 0 until chunksSidesCount)
                for (y in 0 until chunksSidesCount)
                    for (z in 0 until chunksSidesCount) {
                        val inAtlasCoordinateX = (chunkStartX + x) and (chunksSidesCount-1)
                        val inAtlasCoordinateY = (chunkStartY + y) and (chunksSidesCount-1)
                        val inAtlasCoordinateZ = (chunkStartZ + z) and (chunksSidesCount-1)

                        val cacheIndex = ((inAtlasCoordinateX) * chunksSidesCount + inAtlasCoordinateY ) * chunksSidesCount + inAtlasCoordinateZ
                        val cacheEntry = chunksCache[cacheIndex]
                        val chunk = world.getChunk(chunkStartX + x, chunkStartY + y, chunkStartZ + z)

                        if (chunk == null) {
                            chunksCache[cacheIndex] = null
                            continue
                        }
                        else if(chunk != cacheEntry) {
                            chunksCache[cacheIndex] = null
                        }
                        else if(chunk == cacheEntry) {
                            val oldRevision = revisionCache[cacheIndex]
                            val newRevision = chunk.revision.get()

                            //println("$newRevision")
                            if(oldRevision < newRevision) {
                                revisionCache[cacheIndex] = newRevision
                            }
                            else {
                                continue
                            }
                        }

                        //println("Uploading $chunk")

                        chunksCache[cacheIndex] = chunk

                        copies[copiesCount++].apply {
                            bufferOffset(scratchByteBuffer.position().toLong())

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
                                x((chunk.chunkX % chunksSidesCount) * 32)
                                y((chunk.chunkY % chunksSidesCount) * 32)
                                z((chunk.chunkZ % chunksSidesCount) * 32)
                            }

                            imageExtent().apply {
                                width(32)
                                height(32)
                                depth(32)
                            }
                        }

                        extractChunkInBuffer(scratchByteBuffer, chunk)
                    }

            if(copiesCount == 0)
                return

            copies.position(0)
            copies.limit(copiesCount)

            scratchByteBuffer.flip()

            val preUpdateBarrier = VkImageMemoryBarrier.callocStack(1).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                oldLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)

                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                image(texture.imageHandle)

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

            val scratchVkBuffer = VulkanBuffer(backend, scratchByteBuffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT, MemoryUsagePattern.SEMI_STATIC)
            vkCmdCopyBufferToImage(commandBuffer, scratchVkBuffer.handle, texture.imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copies)

            val postUpdateBarrier = VkImageMemoryBarrier.callocStack(1).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                image(texture.imageHandle)

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

            val fence = backend.createFence(false)
            operationsPool.submitOneTimeCB(commandBuffer, backend.logicalDevice.graphicsQueue, fence)
            backend.waitFence(fence)

            vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)
            vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

            scratchVkBuffer.cleanup()
        }
    }

    private fun extractChunkInBuffer(byteBuffer: ByteBuffer, chunk: CubicChunk) {
        val voxelData = chunk.voxelDataArray

        if (voxelData == null) {
            for (i in 0 until 32 * 32 * 32) {
                byteBuffer.put(0)
                byteBuffer.put(0)
                byteBuffer.put(0)
                byteBuffer.put(0)
            }
        } else {
            val color = Vector4f()
            for (z in 0..31)
                for (y in 0..31)
                    for (x in 0..31) {
                        val data = voxelData[x * 32 * 32 + y * 32 + z]
                        val voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(data)) ?: world.content.voxels().air()

                        if (voxel.isAir() || (!voxel.solid && voxel.emittedLightLevel == 0) || voxel.name.startsWith("glass")) {
                            byteBuffer.put(0)
                            byteBuffer.put(0)
                            byteBuffer.put(0)
                            byteBuffer.put(0)

                            if(voxel.name.startsWith("lava"))
                                println("shit" + voxel.emittedLightLevel)
                        } else {
                            val topTexture = voxel.getVoxelTexture(chunk.peek(x, y, z), VoxelSide.TOP)
                            color.set(topTexture.color)

                            if (topTexture.name.equals("grass_top")) {
                                color.set(0.2f, 1.0f, 0.5f, 0.5f)
                            }

                            byteBuffer.put((color.x() * 255).toInt().toByte())
                            byteBuffer.put((color.y() * 255).toInt().toByte())
                            byteBuffer.put((color.z() * 255).toInt().toByte())

                            byteBuffer.put(((0.5 + 0.5 * voxel.emittedLightLevel / 15.0) * 255).toInt().toByte())

                            //if(voxel.emittedLightLevel > 1)
                            //    println(((0.5 + 0.5 * voxel.emittedLightLevel / 15.0) * 255))

                            /*if (topTexture.name.equals("grass_top"))
                                byteBuffer.put((0.5 * 255).toInt().toByte())
                            else
                                byteBuffer.put((color.w() * 255).toInt().toByte())*/
                        }

                    }
        }
    }

    override fun cleanup() {
        texture.cleanup()
        memFree(scratchByteBuffer)
    }
}

class VolumetricTextureMetadata : InterfaceBlock {
    val baseChunkPos = Vector3i(0)
    var size = 64

    var noise = 0
}