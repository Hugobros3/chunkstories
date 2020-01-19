package xyz.chunkstories.graphics.vulkan.resources

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanUniformBuffer
import xyz.chunkstories.graphics.vulkan.textures.*
import xyz.chunkstories.graphics.vulkan.util.VkDescriptorSet
import xyz.chunkstories.graphics.vulkan.util.writeCombinedImageSamplerDescriptor
import xyz.chunkstories.graphics.vulkan.util.writeStorageBufferDescriptor
import xyz.chunkstories.graphics.vulkan.util.writeUniformBufferDescriptor
import java.nio.IntBuffer

/** Thread UNSAFE semi-immediate mode emulation of the conventional binding model */
class VulkanShaderResourcesContext internal constructor(val megapool: DescriptorSetsMegapool, val pipeline: Pipeline) {
    val backend: VulkanGraphicsBackend = megapool.backend
    private val sets = mutableMapOf<Int, VkDescriptorSet>()

    val samplers: VulkanSamplers
        get() = megapool.samplers

    //TODO delete me
    val tempBuffers = mutableListOf<VulkanBuffer>()

    private fun getSet(slot: Int): VkDescriptorSet {
        val slotLayout = pipeline.program.slotLayouts[slot]
        val subpool = megapool.getSubpoolForLayout(slotLayout)
        var set = sets[slot]

        if (set == null) {
            set = subpool.acquireDescriptorSet()
            sets[slot] = set
        }

        return set
    }

    fun bindStructuredUBO(instanceName: String, interfaceBlock: InterfaceBlock) {
        //TODO path w/ name instead of IB class
        val uboBindPoint = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformBlock>().find {
            it.struct.kClass == interfaceBlock.javaClass.kotlin //TODO ::class ?
                    && it.instanceName == instanceName
        } ?: throw Exception("I can't find a program resource matching that interface block :s")

        val set = getSet(uboBindPoint.locator.descriptorSetSlot)

        //TODO UBO MEGAPOOL
        val buffer = VulkanUniformBuffer(backend, uboBindPoint.struct)
        tempBuffers.add(buffer)

        buffer.upload(interfaceBlock)
        backend.writeUniformBufferDescriptor(set, uboBindPoint.locator.binding, buffer, 0, buffer.bufferSize)
    }

    fun bindRawUBO(rawName: String, buffer: VulkanUniformBuffer) {
        val uboBindPoint = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformBlock>().find { it.name == rawName }
                ?: throw Exception("Can't find a program resource matching that name in this context")

        val set = getSet(uboBindPoint.locator.descriptorSetSlot)
        backend.writeUniformBufferDescriptor(set, uboBindPoint.locator.binding, buffer, 0, buffer.bufferSize)
    }

    fun bindInstancedInput(name: String, buffer: VulkanBuffer, offset: Long = 0) {
        val ressource = pipeline.program.glslProgram.instancedInputs.find { it.name == name }!!
        bindInstancedInput(ressource, buffer, offset)
    }

    fun bindInstancedInput(instancedInput: GLSLInstancedInput, buffer: VulkanBuffer, offset: Long = 0) {
        val realResource = instancedInput.associatedResource
        when (realResource) {
            is GLSLShaderStorage -> {
                bindSSBO(realResource, buffer, offset)
            }
            else -> throw Exception("Associated ressource to an instanced input is not a SSBO yet we are in Vulkan mode!")
        }
    }

    fun bindSSBO(name: String, buffer: VulkanBuffer, offset: Long = 0) {
        val ssbo = pipeline.program.glslProgram.resources.filterIsInstance<GLSLShaderStorage>().find {
            it.name == name
        } ?: return // ?: throw Exception("I can't find a program sampler2D resource matching that name '$name'")
        bindSSBO(ssbo, buffer, offset)
    }

    fun bindSSBO(ssbo: GLSLShaderStorage, buffer: VulkanBuffer, offset: Long = 0) {
        val set = getSet(ssbo.locator.descriptorSetSlot)
        backend.writeStorageBufferDescriptor(set, ssbo.locator.binding, buffer, offset)
    }

    fun bindTextureAndSampler(name: String, texture: VulkanTexture2D, sampler: VulkanSampler, index: Int = 0) {
        val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage2D>().find {
            it.name == name
        } ?: return // ?: throw Exception("I can't find a program sampler2D resource matching that name '$name'")

        val set = getSet(resource.locator.descriptorSetSlot)
        backend.writeCombinedImageSamplerDescriptor(set, resource.locator.binding, texture, sampler, index)
    }

    fun bindTextureAndSampler(name: String, texture: VulkanOnionTexture2D, sampler: VulkanSampler, index: Int = 0) {
        val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage2DArray>().find {
            it.name == name
        } ?: return // ?: throw Exception("I can't find a program sampler2DArray resource matching that name '$name'")

        val set = getSet(resource.locator.descriptorSetSlot)
        backend.writeCombinedImageSamplerDescriptor(set, resource.locator.binding, texture, sampler, index)
    }

    fun bindTextureAndSampler(name: String, texture: VulkanTexture3D, sampler: VulkanSampler, index: Int = 0) {
        val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage3D>().find {
            it.name == name
        } ?: return // ?: throw Exception("I can't find a program sampler3D resource matching that name '$name'")

        val set = getSet(resource.locator.descriptorSetSlot)
        backend.writeCombinedImageSamplerDescriptor(set, resource.locator.binding, texture, sampler, index)
    }

    fun bindTextureAndSampler(name: String, texture: VulkanTextureCubemap, sampler: VulkanSampler, index: Int = 0) {
        val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImageCubemap>().find {
            it.name == name
        } ?: return // ?: throw Exception("I can't find a program sampler3D resource matching that name '$name'")

        val set = getSet(resource.locator.descriptorSetSlot)
        backend.writeCombinedImageSamplerDescriptor(set, resource.locator.binding, texture, sampler, index)
    }

    /** Commits the binds and bind appropriate sets to the command buffer */
    fun commitAndBind(commandBuffer: VkCommandBuffer) {
        MemoryStack.stackPush()
        for ((slot, set) in sets.entries) {
            VK10.vkCmdBindDescriptorSets(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipelineLayout, slot, MemoryStack.stackLongs(set), null as? IntBuffer)
        }

        MemoryStack.stackPop()
    }

    fun recycle() {
        var i = 0

        for ((slot, set) in sets) {
            val slotLayout = pipeline.program.slotLayouts[slot]
            val subpool = megapool.getSubpoolForLayout(slotLayout)
            subpool.available.add(set)
            i++
        }

        //println("Recycled $i sets")

        for (buffer in tempBuffers)
            buffer.cleanup()
    }
}