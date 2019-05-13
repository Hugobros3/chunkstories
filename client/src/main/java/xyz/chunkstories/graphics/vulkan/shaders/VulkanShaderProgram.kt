package xyz.chunkstories.graphics.vulkan.shaders

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackCallocInt
import org.lwjgl.vulkan.EXTDescriptorIndexing.*
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfoEXT
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import xyz.chunkstories.api.graphics.shader.ShaderStage
import xyz.chunkstories.api.graphics.structs.UniformUpdateFrequency
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.SpirvCrossHelper
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.textures.MagicTexturing.Companion.magicTexturesNames
import xyz.chunkstories.graphics.vulkan.textures.MagicTexturing.Companion.magicTexturesUpperBound
import xyz.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import xyz.chunkstories.graphics.vulkan.util.ensureIs

data class VulkanShaderProgram internal constructor(val backend: VulkanGraphicsBackend, val glslProgram: GLSLProgram) : Cleanable {
    val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram)
    val modules: Map<ShaderStage, ShaderModule>

    val slotLayouts: Array<DescriptorSlotLayout>

    init {
        MemoryStack.stackPush()

        modules = spirvCode.stages.mapValues { ShaderModule(backend, it.value) }

        slotLayouts = Array(UniformUpdateFrequency.values().size + 2) { slot ->
            DescriptorSlotLayout(createLayoutForSlot(slot), getDescriptorCountByType(slot), slot == 0)
        }

        MemoryStack.stackPop()
    }

    val GLSLResource.vkDescriptorType: Int
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

    val GLSLResource.vkDescriptorCount: Int
        get() = when (this) {
            is GLSLUniformBlock -> 1
            is GLSLUniformSampler -> 1
            is GLSLShaderStorage -> 1
            is GLSLUniformSampledImage2D -> this.count
            is GLSLUniformSampledImage2DArray -> 1
            is GLSLUniformSampledImage3D -> 1
            is GLSLUniformSampledImageCubemap -> 1

            is GLSLUniformImage2D -> if (this.count != 0) this.count else magicTexturesUpperBound
            else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
        }

    private fun getDescriptorCountByType(slot: Int): Map<Int, Int> {
        return glslProgram.resources
                .filter { it.descriptorSetSlot == slot } // Filter only those who match this descriptor set slot
                .mapNotNull { resource ->

                    val descriptorType = resource.vkDescriptorType
                    val descriptorsNeeded = resource.vkDescriptorCount

                    Pair(descriptorType, descriptorsNeeded)
                }.groupBy { it.first }.mapValues { it.value.sumBy { it.second } }
    }

    //TODO create the layout from getDescriptorCountByType(): Map<Int, Int>
    //TODO and reuse the layouts
    private fun createLayoutForSlot(slot: Int): VkDescriptorSetLayout {
        // We want a list of all the bindings the layout for the set #i, we'll start by taking all the shader resources ...
        val bindingsMap = glslProgram.resources
                .filter { it.descriptorSetSlot == slot } // Filter only those who match this descriptor set slot
                .associateWith { resource ->
                    // And depending on their type we'll make them correspond to the relevant Vulkan objects
                    VkDescriptorSetLayoutBinding.callocStack().apply {
                        binding(resource.binding)

                        val descriptorType = resource.vkDescriptorType
                        val descriptorsNeeded = resource.vkDescriptorCount

                        descriptorType(descriptorType)
                        descriptorCount(descriptorsNeeded)

                        if (slot == 0 && resource.name in magicTexturesNames) {
                            // we can't set the binding flags here, see VkDescriptorSetLayoutBindingFlagsCreateInfoEXT below
                        }

                        stageFlags(VK10.VK_SHADER_STAGE_ALL_GRAPHICS) //TODO we could be more precise here
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
            if (slot == 0)
                flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT_EXT)
        }

        if (slot == 0 && backend.logicalDevice.enableMagicTexturing) {
            val bindingFlags = stackCallocInt(bindingsMap.size)
            for ((resource, _) in bindingsMap) {
                bindingFlags.put(when (resource) {
                    is GLSLUniformImage2D ->
                        VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT_EXT or
                                VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT_EXT or
                                VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT_EXT
                    else -> 0
                })
            }
            bindingFlags.flip()

            val bindingFlagsCreateInfo = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.callocStack().apply {
                sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO_EXT)

                bindingCount(2)
                pBindingFlags(bindingFlags)
                pNext(setLayoutCreateInfo.pNext())
            }

            setLayoutCreateInfo.pNext(bindingFlagsCreateInfo.address())
        }

        val pDescriptorSetLayout = MemoryStack.stackLongs(1)
        vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayoutCreateInfo, null, pDescriptorSetLayout)
                .ensureIs("Failed to create descriptor set layout", VK_SUCCESS)

        return pDescriptorSetLayout.get(0)
    }

    override fun cleanup() {
        modules.values.forEach { it.cleanup() }
        slotLayouts.forEach { it.cleanup(backend) }
    }
}