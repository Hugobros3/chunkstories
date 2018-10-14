package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.api.graphics.structs.UniformUpdateFrequency
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanUniformBuffer
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.textures.VulkanSampler
import io.xol.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorPool
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

/** Holds the descriptors a shader program needs */
class DescriptorPool(val backend: VulkanGraphicsBackend, val program: VulkanShaderFactory.VulkanicShaderProgram) : Cleanable {
    val handle: VkDescriptorPool

    internal val descriptorSets: LongArray

    internal val ubos = mutableMapOf<ShaderFactory.GLSLUniformBlock, Array<VulkanUniformBuffer>>()

    init {
        stackPush()

        // Contains a list of all the descriptor types we need, where the number of duplicates tells us how many program resources need one
        val resourcesTypes = program.glslProgram.resources.mapNotNull {
            when (it) {
                is ShaderFactory.GLSLUniformBlock -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                is ShaderFactory.GLSLUniformSampler2D -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                else -> null
            }
        }

        if (resourcesTypes.isNotEmpty()) {

            val resourcesSize = VkDescriptorPoolSize.callocStack(resourcesTypes.size)
            resourcesTypes.toSet().mapIndexed { index, type ->
                resourcesSize[index].apply {
                    // VK_DESCRIPTOR_SOMETHING
                    type(type)

                    // Ie we have 4 ubos in the shader, and 3 swapchain images, so we need 12 descriptors
                    val numberOfResourcesOfThatType = resourcesTypes.count { it == type }
                    descriptorCount(numberOfResourcesOfThatType * backend.swapchain.maxFramesInFlight)
                    println("Asked for ${descriptorCount()} : $type")
                }
            }

            val descriptorSetsCount = (1 + UniformUpdateFrequency.values().size) * backend.swapchain.maxFramesInFlight

            // Create the pool at last
            val poolCreateInfo = VkDescriptorPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).apply {
                pPoolSizes(resourcesSize)
                maxSets(descriptorSetsCount)
            }

            val pDescriptorPool = stackMallocLong(1)
            vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pDescriptorPool)
                    .ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
            handle = pDescriptorPool.get(0)

            // Create those descriptor sets immediately
            // TODO optimize this and have spare descriptor sets to bulk-update ?
            // TODO probably unnecessary as UBOs are meant to be mostly static but idk
            val layouts = stackMallocLong(descriptorSetsCount)

            for (setLayout in program.descriptorSetLayouts) {
                // We want a descriptor set for every set in use and for every possible frame in flight
                for (i in 0 until backend.swapchain.maxFramesInFlight)
                    layouts.put(setLayout)
            }
            layouts.flip()

            val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
                descriptorPool(handle)
                pSetLayouts(layouts)
            }

            val pDescriptorSets = stackMallocLong(descriptorSetsCount)
            vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)
            descriptorSets = LongArray(descriptorSetsCount)
            pDescriptorSets.get(descriptorSets)

        } else {
            // In the weird case where we don't have any uniforms, we create a dummy descriptor pool
            handle = -1L
            descriptorSets = LongArray(0)
        }

        stackPop()
    }

    fun configure(frame: Frame, interfaceBlock: InterfaceBlock) {
        val resource = program.glslProgram.resources.filterIsInstance<ShaderFactory.GLSLUniformBlock>().find {
            it.mapper.klass == interfaceBlock.javaClass.kotlin //TODO ::class ?
        } ?: throw Exception("I can't find a program resource matching that interface block :s")

        stackPush()

        // Locate (or create!) the buffer that actually holds the UBO data
        val uboContents = ubos.getOrPut(resource) {
            // Creates N buffers holding the UBO data
            Array(backend.swapchain.maxFramesInFlight) {
                VulkanUniformBuffer(backend, resource.mapper)
            }
        }[frame.inflightFrameIndex]

        // Update it !
        uboContents.upload(interfaceBlock)

        // Now we just have to update the descriptor in the right set...
        // First, a struct that points the actual data
        val bufferInfo = VkDescriptorBufferInfo.callocStack(1).apply {
            buffer(uboContents.handle)
            offset(0)
            range(VK_WHOLE_SIZE)
        }

        // Update *that* set
        val destinationSet = descriptorSets[resource.descriptorSet * backend.swapchain.maxFramesInFlight + frame.inflightFrameIndex]

        // Just update it
        val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
            dstSet(destinationSet)
            dstBinding(resource.binding)
            dstArrayElement(0)
            descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

            // Just update the descriptor for our lone ubo buffer
            pBufferInfo(bufferInfo)
        }

        vkUpdateDescriptorSets(backend.logicalDevice.vkDevice, stuffToWrite, null)

        stackPop()
    }

    fun configureTextureAndSampler(frame: Frame, name: String, texture: VulkanTexture2D, sampler: VulkanSampler) {
        val resource = program.glslProgram.resources.filterIsInstance<ShaderFactory.GLSLUniformSampler2D>().find {
            it.name == name
        } ?: throw Exception("I can't find a program sampler2D resource matching that name '$name' :s")

        stackPush()

        val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
            imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
            imageView(texture.imageView)
            sampler(sampler.handle)
        }

        val destinationSet = descriptorSets[frame.inflightFrameIndex] // We store all the samplers in descriptor set zero for now

        val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
            dstSet(destinationSet)
            dstBinding(resource.binding)
            dstArrayElement(0)
            descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)

            // Just update the descriptor for our lone ubo buffer
            pImageInfo(imageInfo)
        }

        vkUpdateDescriptorSets(backend.logicalDevice.vkDevice, stuffToWrite, null)

        stackPop()
    }

    /** Returns an array containing the handles of the descriptor sets to bind at frame N */
    fun setsForFrame(frame: Frame): LongArray {
        val setsCount = (1 + UniformUpdateFrequency.values().size)
        val sets = LongArray(setsCount)

        for (setIndex in 0 until setsCount) {
            val set = descriptorSets[setIndex * backend.swapchain.maxFramesInFlight + frame.inflightFrameIndex]
            sets[setIndex] = set
        }

        return sets
    }

    override fun cleanup() {
        if (handle != -1L)
            vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, handle, null)

        // Free all those buffers
        ubos.values.forEach { it.forEach { it.cleanup() } }
    }
}
