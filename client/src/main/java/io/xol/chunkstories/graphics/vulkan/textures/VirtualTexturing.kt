package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.system.MemoryStack.*

import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*

class VirtualTexturing(val backend: VulkanGraphicsBackend, val program: ShaderFactory.GLSLProgram) {

    companion object {
        const val MAX_VIRTUAL_TEXTURING_ARRAY_SIZE = 512L

        fun getNumberOfSlotsForVirtualTexturing(resources: List<ShaderFactory.GLSLUniformResource>) : Int {
            stackPush()

            val physicalDeviceProperties = VkPhysicalDeviceProperties.callocStack()
            vkGetPhysicalDeviceProperties(backend.physicalDevice.vkPhysicalDevice, physicalDeviceProperties)

            val reservedForUBOs = resources.count { it is ShaderFactory.GLSLUniformBlock }
            val reservedForNonVirtualTextureInputs = resources.count { it is ShaderFactory.GLSLUniformSampler2D }

            println("${physicalDeviceProperties.deviceNameString()} : ${physicalDeviceProperties.limits().maxPerStageResources().toUInt().toLong()}")

            val limits = physicalDeviceProperties.limits()

            val possibleLimits = listOf(
                    // We can't have more combined samplers than we have for the individual resources
                    // Plus we need to reserve a bunch of them
                    limits.maxDescriptorSetSamplers().toUInt().toLong() - reservedForNonVirtualTextureInputs,
                    limits.maxDescriptorSetSampledImages().toUInt().toLong() - reservedForNonVirtualTextureInputs,
                    limits.maxPerStageDescriptorSamplers().toUInt().toLong()- reservedForNonVirtualTextureInputs,
                    limits.maxPerStageDescriptorSampledImages().toUInt().toLong() - reservedForNonVirtualTextureInputs,

                    /* This one is weirder: it is possible that the maximum number of bound resources is lower than the sum of
                     the limits of each individual resource type. In this case we need to ensure we still have enough descriptors
                     for UBOs and virtual texturing stuff  */
                    (limits.maxPerStageResources().toUInt().toLong() - reservedForUBOs - reservedForNonVirtualTextureInputs)
            )

            println(possibleLimits)

            val virtualTexturingSlots = Math.min(possibleLimits.min()!!, MAX_VIRTUAL_TEXTURING_ARRAY_SIZE).toInt()
            if(virtualTexturingSlots <= 0)
                throw Exception("Oops! This device doesn't have the ability to bind everything we need for this shader sadly !")

            stackPop()

            return virtualTexturingSlots
        }
    }
}