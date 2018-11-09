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
class DescriptorPool(val backend: VulkanGraphicsBackend, val program: VulkanShaderFactory.VulkanShaderProgram) : Cleanable {
    val handle: VkDescriptorPool

    // Array of array of descriptor sets
    // First dimension is the descriptor set slot, second dimension is for the current frame in flight
    // Gotcha: Index 0 of the primary array corresponds to the descriptor set 1, as binding 0 is reserved for virtual texturing
    internal val allocatedSets: Map<Int, LongArray>

    // We also store the contents for the UBOs here
    internal val ubos = mutableMapOf<ShaderFactory.GLSLUniformBlock, Array<VulkanUniformBuffer>>()

    init {
        stackPush()

        val descriptorsCountPerType = mutableMapOf<Int, Int>()
        resources@ for(resource in program.glslProgram.resources ) {
            // Ignore resources from descriptor set zero when allocating this pool
            if(resource.descriptorSet == 0)
                continue

            val descriptorType = when (resource) {
                is ShaderFactory.GLSLUniformBlock -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
                is ShaderFactory.GLSLUniformSampler2D -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                is ShaderFactory.GLSLUnusedUniform -> continue@resources
                else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
            }

            val descriptorsNeeded = when (resource) {
                is ShaderFactory.GLSLUniformBlock -> 1
                is ShaderFactory.GLSLUniformSampler2D -> resource.count
                else -> throw Exception("Missing mapping from GLSLResource type to Vulkan descriptor type !")
            }

            val descriptorsForThatResourceType = descriptorsCountPerType[descriptorType] ?: 0
            descriptorsCountPerType[descriptorType] = descriptorsForThatResourceType + descriptorsNeeded
        }

        if (descriptorsCountPerType.isNotEmpty()) {
            val resourcesSize = VkDescriptorPoolSize.callocStack(descriptorsCountPerType.keys.size)
            descriptorsCountPerType.entries.forEachIndexed { index, (descriptorType, count) ->
                resourcesSize[index].apply {
                    // VK_DESCRIPTOR_SOMETHING
                    type(descriptorType)

                    // Ie we have 4 ubos in the shader, and 3 swapchain images, so we need 12 descriptors
                    descriptorCount(count * backend.swapchain.maxFramesInFlight)
                    println("Asked for ${descriptorCount()} of $descriptorType")
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

            // Allocate every set we need from this pool
            val layouts = stackMallocLong(descriptorSetsCount)
            for (setLayoutIndex in 1 until (UniformUpdateFrequency.values().size) + 2) {
                for (j in 0 until backend.swapchain.maxFramesInFlight)
                    layouts.put(program.descriptorSetLayouts[setLayoutIndex])
            }
            layouts.flip()

            val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
                descriptorPool(handle)
                pSetLayouts(layouts)
            }

            val pDescriptorSets = stackMallocLong(descriptorSetsCount)
            vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)

            allocatedSets = mutableMapOf()

            for(set in 1 until UniformUpdateFrequency.values().size + 2) {
                val instances = LongArray(backend.swapchain.maxFramesInFlight)
                pDescriptorSets.get(instances)
                allocatedSets[set] = instances
            }
        } else {
            // In the weird case where we don't have any uniforms, we create a dummy descriptor pool
            handle = -1L
            allocatedSets = mapOf()
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
        val destinationSet = allocatedSets[resource.descriptorSet]!![frame.inflightFrameIndex]

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

        val destinationSet = allocatedSets[1]!![frame.inflightFrameIndex] // We store all the samplers in descriptor set zero for now

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

    /** Returns an array containing the handles of the descriptor sets to bind at frame N (except for VT) */
    fun setsForFrame(frame: Frame): LongArray {
        val sets2returnCount = (UniformUpdateFrequency.values().size + 1)
        val sets = LongArray(sets2returnCount)

        for (setIndex in 0 until sets2returnCount) {
            val set = allocatedSets[setIndex + 1]!![frame.inflightFrameIndex]
            //println(set)
            sets[setIndex] = set
        }

        return sets
    }

    override fun cleanup() {
        // Destroying the pool also kills all the descriptors, so that's nice
        if (handle != -1L)
            vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, handle, null)

        // Free all those buffers
        ubos.values.forEach { it.forEach { it.cleanup() } }
    }
}
