package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.VkSampler
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSamplerCreateInfo

class VulkanSampler(val backend: VulkanGraphicsBackend) : Cleanable {
    val handle : VkSampler

    init {
        stackPush()
        val samplerInfo = VkSamplerCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO).apply {
            magFilter(VK_FILTER_NEAREST)
            minFilter(VK_FILTER_NEAREST)
            addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
            addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
            addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)

            anisotropyEnable(false)
            maxAnisotropy(1F) // 16F

            unnormalizedCoordinates(false)

            compareEnable(false)
            compareOp(VK_COMPARE_OP_ALWAYS) //TODO shadowmap pcf will go here

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