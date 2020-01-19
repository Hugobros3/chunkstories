package xyz.chunkstories.graphics.vulkan.shaders

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import xyz.chunkstories.graphics.vulkan.util.ensureIs

data class DescriptorSlotLayout constructor(private val backend: VulkanGraphicsBackend, val resources: Set<GLSLResource>) : Cleanable {
    val vkLayoutHandle: VkDescriptorSetLayout = createLayoutForSlot(resources)

    val descriptorsCountByType = getDescriptorCountByType(resources)

    override fun cleanup() {
        vkDestroyDescriptorSetLayout(backend.logicalDevice.vkDevice, vkLayoutHandle, null)
    }

    private fun createLayoutForSlot(resources: Set<GLSLResource>): VkDescriptorSetLayout {
        stackPush()
        // We want a list of all the bindings the layout for the set #i, we'll start by taking all the shader resources ...
        val bindingsMap = resources
                .associateWith { resource ->
                    // And depending on their type we'll make them correspond to the relevant Vulkan objects
                    VkDescriptorSetLayoutBinding.callocStack().apply {
                        binding(resource.locator.binding)

                        val descriptorType = resource.vkDescriptorType
                        val descriptorsNeeded = resource.vkDescriptorCount

                        descriptorType(descriptorType)
                        descriptorCount(descriptorsNeeded)

                        stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS) //TODO we could be more precise here
                        //pImmutableSamplers() //TODO
                    }
                }

        // (Transforming the above struct into native-friendly stuff )
        val pBindings = if (bindingsMap.isNotEmpty()) {
            val them = VkDescriptorSetLayoutBinding.callocStack(bindingsMap.size)
            bindingsMap.forEach { (_, it) -> them.put(it) }
            them
        } else null
        pBindings?.flip()

        val setLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).apply {
            pBindings(pBindings)
        }

        val pDescriptorSetLayout = MemoryStack.stackLongs(1)
        vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayoutCreateInfo, null, pDescriptorSetLayout)
                .ensureIs("Failed to create descriptor set layout", VK_SUCCESS)

        val layout = pDescriptorSetLayout.get(0)
        stackPop()
        return layout
    }

    private fun getDescriptorCountByType(resources: Set<GLSLResource>): Map<Int, Int> {
        return resources.map { resource ->

            val descriptorType = resource.vkDescriptorType
            val descriptorsNeeded = resource.vkDescriptorCount

            Pair(descriptorType, descriptorsNeeded)
        }.groupBy { it.first }.mapValues { it.value.sumBy { it.second } }
    }

    private val GLSLResource.vkDescriptorType: Int
        get() = when (this) {
            is GLSLUniformBlock -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
            is GLSLUniformImage2D -> VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE
            is GLSLShaderStorage -> VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
            is GLSLUniformSampledImage2D -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
            is GLSLUniformSampledImage3D -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
            is GLSLUniformSampledImage2DArray -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
            is GLSLUniformSampledImageCubemap -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
            is GLSLUniformSampler -> VK_DESCRIPTOR_TYPE_SAMPLER
            //is GLSLUnusedUniform -> return@mapNotNull null
            else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
        }

    private val GLSLResource.vkDescriptorCount: Int
        get() = when (this) {
            is GLSLUniformBlock -> 1
            is GLSLUniformSampler -> 1
            is GLSLShaderStorage -> 1
            is GLSLUniformSampledImage2D -> this.count
            is GLSLUniformSampledImage2DArray -> 1
            is GLSLUniformSampledImage3D -> 1
            is GLSLUniformSampledImageCubemap -> 1

            is GLSLUniformImage2D -> this.count
            else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
        }
}