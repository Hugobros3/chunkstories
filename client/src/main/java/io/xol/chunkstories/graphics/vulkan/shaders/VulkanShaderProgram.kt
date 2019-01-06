package io.xol.chunkstories.graphics.vulkan.shaders

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.UniformUpdateFrequency
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

data class VulkanShaderProgram internal constructor(val backend: VulkanGraphicsBackend, val glslProgram: ShaderFactory.GLSLProgram) {
    val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram)
    val modules: Map<ShaderStage, ShaderModule>

    val slotLayouts: Array<DescriptorSlotLayout>

    init {
        MemoryStack.stackPush()

        modules = spirvCode.stages.mapValues { ShaderModule(backend, it.value) }

        slotLayouts = Array(UniformUpdateFrequency.values().size + 2) { slot ->
            DescriptorSlotLayout(createLayoutForSlot(slot), getDescriptorCountByType(slot))
        }

        MemoryStack.stackPop()
    }

    private fun getDescriptorCountByType(slot: Int): Map<Int, Int> {
        return glslProgram.resources
                .filter { it.descriptorSetSlot == slot } // Filter only those who match this descriptor set slot
                .mapNotNull { resource ->
                    val descriptorType = when (resource) {
                        is ShaderFactory.GLSLUniformBlock -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                        is ShaderFactory.GLSLUniformSampler2D -> VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                        is ShaderFactory.GLSLUnusedUniform -> return@mapNotNull null
                        else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
                    }

                    val descriptorsNeeded = when (resource) {
                        is ShaderFactory.GLSLUniformBlock -> 1
                        is ShaderFactory.GLSLUniformSampler2D -> resource.count
                        else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
                    }

                    Pair(descriptorType, descriptorsNeeded)
                }.toMap()
    }

    //TODO create the layout from getDescriptorCountByType(): Map<Int, Int>
    //TODO and reuse the layouts
    private fun createLayoutForSlot(slot: Int) : VkDescriptorSetLayout {
        // We want a list of all the bindings the layout for the set #i, we'll start by taking all the shader resources ...
        val layoutBindings = glslProgram.resources
                .filter { it.descriptorSetSlot == slot } // Filter only those who match this descriptor set slot
                .mapNotNull { resource ->
                    // And depending on their type we'll make them correspond to the relevant Vulkan objects

                    if (resource is ShaderFactory.GLSLUnusedUniform)
                        return@mapNotNull null

                    VkDescriptorSetLayoutBinding.callocStack().apply {
                        binding(resource.binding)

                        descriptorType(when (resource) {
                            is ShaderFactory.GLSLUniformSampler2D -> VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                            is ShaderFactory.GLSLUniformBlock -> VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                            else -> throw Exception("Unmappped GLSL Uniform resource type")
                        })

                        descriptorCount(when (resource) {
                            is ShaderFactory.GLSLUniformSampler2D -> resource.count
                            else -> 1 //TODO maybe allow arrays of ubo ? not for now
                        })

                        stageFlags(VK10.VK_SHADER_STAGE_ALL_GRAPHICS) //TODO we could be more precise here
                        //pImmutableSamplers() //TODO
                    }
                }

        // (Transforming the above struct into native-friendly stuff )
        val pLayoutBindings = if (layoutBindings.isNotEmpty()) {
            val them = VkDescriptorSetLayoutBinding.callocStack(layoutBindings.size)
            layoutBindings.forEach { them.put(it) }
            them
        } else null
        pLayoutBindings?.flip()

        val setLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).apply {
            pBindings(pLayoutBindings)
        }

        val pDescriptorSetLayout = MemoryStack.stackLongs(1)
        VK10.vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayoutCreateInfo, null, pDescriptorSetLayout)
                .ensureIs("Failed to create descriptor set layout", VK10.VK_SUCCESS)

        return pDescriptorSetLayout.get(0)
    }

    fun cleanup() {
        modules.values.forEach { it.cleanup() }

        //descriptorSetLayouts.forEach { vkDestroyDescriptorSetLayout(backend.logicalDevice.vkDevice, it, null) }
    }
}