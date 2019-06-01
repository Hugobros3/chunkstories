package xyz.chunkstories.graphics.vulkan.textures

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkSampler
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSamplerCreateInfo
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput

class

VulkanSampler(val backend: VulkanGraphicsBackend,
                    val scalingMode: ImageInput.ScalingMode = ImageInput.ScalingMode.NEAREST,
                    val depthCompareMode: ImageInput.DepthCompareMode = ImageInput.DepthCompareMode.DISABLED,
                    val tilingMode: TextureTilingMode = TextureTilingMode.CLAMP_TO_EDGE)  : Cleanable {
    val handle : VkSampler

    init {
        stackPush()
        val samplerInfo = VkSamplerCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO).apply {
            val filter = when(scalingMode) {
                ImageInput.ScalingMode.LINEAR -> VK_FILTER_LINEAR
                ImageInput.ScalingMode.NEAREST -> VK_FILTER_NEAREST
            }
            magFilter(filter)
            minFilter(filter)

            val addressMode = when(tilingMode) {
                TextureTilingMode.CLAMP_TO_EDGE -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE
                TextureTilingMode.REPEAT -> VK_SAMPLER_ADDRESS_MODE_REPEAT
            }
            addressModeU(addressMode)
            addressModeV(addressMode)
            addressModeW(addressMode)

            anisotropyEnable(false)
            maxAnisotropy(1F) // 16F

            unnormalizedCoordinates(false)

            when (depthCompareMode) {
                ImageInput.DepthCompareMode.DISABLED -> {
                    compareEnable(false)
                    compareOp(VK_COMPARE_OP_ALWAYS)
                }
                //magFilter(VK_FILTER_LINEAR)
                //minFilter(VK_FILTER_LINEAR)
                ImageInput.DepthCompareMode.GREATER -> {
                    compareEnable(true)
                    compareOp(VK_COMPARE_OP_GREATER)
                }
                ImageInput.DepthCompareMode.GREATER_OR_EQUAL -> {
                    compareEnable(true)
                    compareOp(VK_COMPARE_OP_GREATER_OR_EQUAL)
                }
                ImageInput.DepthCompareMode.EQUAL -> {
                    compareEnable(true)
                    compareOp(VK_COMPARE_OP_EQUAL)
                }
                ImageInput.DepthCompareMode.LESS_OR_EQUAL -> {
                    compareEnable(true)
                    compareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
                }
                ImageInput.DepthCompareMode.LESS -> {
                    compareEnable(true)
                    compareOp(VK_COMPARE_OP_LESS)
                }
            }

            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
            mipLodBias(0.0F)
            minLod(0.0F)
            maxLod(0.0F)
        }

        val pSampler = stackMallocLong(1)
        vkCreateSampler(backend.logicalDevice.vkDevice, samplerInfo, null, pSampler).ensureIs("Failed to create sampler", VK_SUCCESS)
        handle = pSampler.get(0)

        stackPop()
    }

    override fun cleanup() {
        vkDestroySampler(backend.logicalDevice.vkDevice, handle, null)
    }
}