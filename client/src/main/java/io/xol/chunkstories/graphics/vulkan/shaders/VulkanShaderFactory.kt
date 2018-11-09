package io.xol.chunkstories.graphics.vulkan.shaders

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.UniformUpdateFrequency
import io.xol.chunkstories.content.mods.ModsManagerImplementation
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

class VulkanShaderFactory(val backend: VulkanGraphicsBackend, val client: Client) : ShaderFactory(VulkanShaderFactory::class.java.classLoader) {
    override val classLoader: ClassLoader
        //Note: We NEED that ?. operator because we're calling into this before Client is done initializing
        get() = (client?.content?.modsManager() as? ModsManagerImplementation)?.finalClassLoader ?: VulkanShaderFactory::class.java.classLoader

    fun loadProgram(basePath: String): GLSLProgram {
        val vertexShader = javaClass.getResource("$basePath.vert").readText()
        val fragmentShader = javaClass.getResource("$basePath.frag").readText()

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return try {
            translateGLSL(GLSLDialect.VULKAN, stages)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load program $basePath, $e")
        }
    }

    fun createProgram(backend: VulkanGraphicsBackend, basePath: String) = VulkanShaderProgram(backend, loadProgram(basePath))

    data class VulkanShaderProgram internal constructor(val backend: VulkanGraphicsBackend, val glslProgram: GLSLProgram) {
        val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram)
        val modules: Map<ShaderStage, ShaderModule>

        val descriptorSetLayouts: Array<VkDescriptorSetLayout>

        init {
            stackPush()

            modules = spirvCode.stages.mapValues { ShaderModule(backend, it.value) }

            // Constructs layouts for the sets we'll be using
            // Important: the descriptor set 0 is reserved for virtual texturing, set 1 is for static samplers and UBOs start at 2
            descriptorSetLayouts = (0 until UniformUpdateFrequency.values().size + 2).map { descriptorSet ->

                // Create bindings for all the resources in that set
                val layoutBindings = glslProgram.resources.filter { it.descriptorSet == descriptorSet }.mapNotNull { resource ->
                    if(resource is GLSLUnusedUniform)
                        return@mapNotNull null

                    VkDescriptorSetLayoutBinding.callocStack().apply {
                        binding(resource.binding)

                        descriptorType(when (resource) {
                            is GLSLUniformSampler2D -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                            is GLSLUniformBlock -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                            else -> throw Exception("Unmappped GLSL Uniform resource type")
                        })

                        descriptorCount(when (resource) {
                            is GLSLUniformSampler2D -> resource.count
                            else -> 1 //TODO maybe allow arrays of ubo ? not for now
                        })

                        stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS) //TODO we could be more precise here
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

                val setLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).apply {
                    pBindings(pLayoutBindings)
                }

                val pDescriptorSetLayout = stackLongs(1)
                vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayoutCreateInfo, null, pDescriptorSetLayout)
                        .ensureIs("Failed to create descriptor set layout", VK_SUCCESS)

                pDescriptorSetLayout.get(0)
            }.toTypedArray()

            stackPop()
        }

        fun cleanup() {
            modules.values.forEach { it.cleanup() }
            descriptorSetLayouts.forEach { vkDestroyDescriptorSetLayout(backend.logicalDevice.vkDevice, it, null) }
        }
    }
}