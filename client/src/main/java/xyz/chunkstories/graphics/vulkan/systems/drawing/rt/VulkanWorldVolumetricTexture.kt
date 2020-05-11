package xyz.chunkstories.graphics.vulkan.systems.drawing.rt

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
import xyz.chunkstories.world.chunk.ChunkImplementation
import java.nio.ByteBuffer

class VulkanWorldVolumetricTexture(val backend: VulkanGraphicsBackend, val world: WorldClientCommon, val volumeSideLength: Int) : Cleanable {
    val mipLevels = 6
    val texture = VulkanTexture3D(backend, TextureFormat.RGBA_8, volumeSideLength, volumeSideLength, volumeSideLength, mipLevels, VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)

    val chunksSidesCount = volumeSideLength / 32
    val singleChunkSizeInRam = 32 * 32 * 32 * 4
    val scratchByteBuffer = memAlloc((chunksSidesCount * chunksSidesCount * chunksSidesCount * singleChunkSizeInRam * 1.5f).toInt())

    val info = VolumetricTextureMetadata()

    val chunksCache = arrayOfNulls<Chunk>(chunksSidesCount * chunksSidesCount * chunksSidesCount)
    val revisionCache = LongArray(chunksCache.size)

    init {
        texture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
    }

    //val lastPos = Vector3i(0)

    fun updateArround(position: Vector3dc) {
        val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
        val commandBuffer = operationsPool.startPrimaryCommandBuffer()

        try {
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

                info.size = volumeSideLength

                val copies = VkBufferImageCopy.calloc(chunksSidesCount * chunksSidesCount * chunksSidesCount * mipLevels)
                var copiesCount = 0
                for (x in 0 until chunksSidesCount)
                    for (y in 0 until chunksSidesCount)
                        for (z in 0 until chunksSidesCount) {
                            val inAtlasCoordinateX = (chunkStartX + x) and (chunksSidesCount - 1)
                            val inAtlasCoordinateY = (chunkStartY + y) and (chunksSidesCount - 1)
                            val inAtlasCoordinateZ = (chunkStartZ + z) and (chunksSidesCount - 1)

                            val cacheIndex = ((inAtlasCoordinateX) * chunksSidesCount + inAtlasCoordinateY) * chunksSidesCount + inAtlasCoordinateZ
                            val cacheEntry = chunksCache[cacheIndex]
                            val chunk = world.chunksManager.getChunk(chunkStartX + x, chunkStartY + y, chunkStartZ + z)

                            if (chunk == null) {
                                chunksCache[cacheIndex] = null
                                continue
                            } else if (chunk != cacheEntry) {
                                chunksCache[cacheIndex] = null
                            } else if (chunk == cacheEntry) {
                                val oldRevision = revisionCache[cacheIndex]
                                val newRevision = chunk.revision.get()

                                //println("$newRevision")
                                if (oldRevision < newRevision) {
                                    revisionCache[cacheIndex] = newRevision
                                } else {
                                    continue
                                }
                            }

                            //println("Uploading $chunk")

                            chunksCache[cacheIndex] = chunk

                            copiesCount = handleChunk(copies, copiesCount, chunk)
                        }

                if (copiesCount == 0)
                    return

                copies.position(0)
                copies.limit(copiesCount)

                scratchByteBuffer.flip()

                val preUpdateBarrier = VkImageMemoryBarrier.callocStack(1).also {
                    it.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                    it.oldLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    it.newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)

                    it.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    it.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    it.image(texture.imageHandle)

                    it.subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(mipLevels)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    it.srcAccessMask(0)
                    it.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                }
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, preUpdateBarrier)

                val scratchVkBuffer = VulkanBuffer(backend, scratchByteBuffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT, MemoryUsagePattern.SEMI_STATIC)
                vkCmdCopyBufferToImage(commandBuffer, scratchVkBuffer.handle, texture.imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copies)

                val postUpdateBarrier = VkImageMemoryBarrier.callocStack(1).also {
                    it.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)

                    it.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    it.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

                    it.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    it.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    it.image(texture.imageHandle)

                    it.subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(mipLevels)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    it.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    it.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                }
                vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, postUpdateBarrier)

                val fence = backend.createFence(false)
                vkEndCommandBuffer(commandBuffer)
                operationsPool.submitAndReturnPrimaryCommandBuffer(commandBuffer, backend.logicalDevice.graphicsQueue, fence)
                backend.waitFence(fence)

                copies.free()

                vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)

                //vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

                scratchVkBuffer.cleanup()
            }
        } finally {
            operationsPool.returnPrimaryCommandBuffer(commandBuffer)
        }
    }

    private fun handleChunk(copies: VkBufferImageCopy.Buffer, copiesCount: Int, chunk: ChunkImplementation): Int {
        var copiesCount1 = copiesCount

        val base = scratchByteBuffer.position().toLong()
        val basePtrs = LongArray(6)
        basePtrs[0] = base
        copies[copiesCount1++].apply {
            bufferOffset(base)

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

        fun isOccluded(level: Int, x: Int, y: Int, z: Int): Boolean {
            val index = basePtrs[level - 1] + ((((z * (32 shr (level - 1))) + y) * (32 shr (level - 1))) + x) * 4 + 3
            return scratchByteBuffer.get(index.toInt()) > 0
        }

        for (mipLevel in 1..5) {
            basePtrs[mipLevel] = scratchByteBuffer.position().toLong()

            copies[copiesCount1++].apply {
                bufferOffset(basePtrs[mipLevel])

                // tightly packed
                bufferRowLength(0)
                bufferImageHeight(0)

                imageSubresource().apply {
                    aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    mipLevel(mipLevel)
                    baseArrayLayer(0)
                    layerCount(1)
                }

                imageOffset().apply {
                    x((chunk.chunkX % chunksSidesCount) * (32 shr mipLevel))
                    y((chunk.chunkY % chunksSidesCount) * (32 shr mipLevel))
                    z((chunk.chunkZ % chunksSidesCount) * (32 shr mipLevel))
                }

                imageExtent().apply {
                    width(32 shr mipLevel)
                    height(32 shr mipLevel)
                    depth(32 shr mipLevel)
                }
            }

            for (z in 0 until (32 shr mipLevel)) {
                for (y in 0 until (32 shr mipLevel)) {
                    for (x in 0 until (32 shr mipLevel)) {

                        var occluded = false
                        for(fz in (z shl 1)..((z shl 1) + 1)) {
                            for(fy in (y shl 1)..((y shl 1) + 1)) {
                                for(fx in (x shl 1)..((x shl 1) + 1)) {
                                    occluded = occluded || isOccluded(mipLevel, fx, fy, fz)
                                }
                            }
                        }

                        scratchByteBuffer.put((if(occluded) 255 else 0).toByte())
                        scratchByteBuffer.put((if(occluded) 255 else 0).toByte())
                        scratchByteBuffer.put((if(occluded) 255 else 0).toByte())
                        scratchByteBuffer.put((if(occluded) 127 else 0).toByte())
                        //print((if(occluded) 127 else 0).toByte())
                    }
                }
            }
        }

        return copiesCount1
    }

    private fun extractChunkInBuffer(byteBuffer: ByteBuffer, chunk: ChunkImplementation) {
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
                        val voxel = world.contentTranslator.getVoxelForId(VoxelFormat.id(data)) ?: world.content.voxels.air

                        if (voxel.isAir() || (!voxel.solid && voxel.emittedLightLevel == 0) || voxel.name.startsWith("glass")) {
                            byteBuffer.put(0)
                            byteBuffer.put(0)
                            byteBuffer.put(0)
                            byteBuffer.put(0)

                            if (voxel.name.startsWith("lava"))
                                println("shit" + voxel.emittedLightLevel)
                        } else {
                            val topTexture = voxel.getVoxelTexture(chunk.peek(x, y, z), VoxelSide.TOP)
                            color.set(topTexture.color)

                            if (topTexture.name.equals("grass_top")) {
                                color.set(0.4f, 0.8f, 0.4f, 1.0f)
                            }

                            byteBuffer.put((color.x() * 255).toInt().toByte())
                            byteBuffer.put((color.y() * 255).toInt().toByte())
                            byteBuffer.put((color.z() * 255).toInt().toByte())

                            byteBuffer.put(((0.5 + 0.5 * voxel.emittedLightLevel / 15.0) * 255).toInt().toByte())
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