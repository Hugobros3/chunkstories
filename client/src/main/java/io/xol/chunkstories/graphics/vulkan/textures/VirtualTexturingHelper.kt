package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.resources.InflightFrameResource
import io.xol.chunkstories.graphics.vulkan.shaders.VulkanShaderFactory
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorPool
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VirtualTexturingHelper(val backend: VulkanGraphicsBackend, val program: VulkanShaderFactory.VulkanShaderProgram) : Cleanable {

    val SLICE_SIZE: Int

    val pool: VkDescriptorPool
    val sets: InflightFrameResource<LongArray>

    val samplers: Array<VulkanSampler>

    init {
        stackPush()

        val vtResource = program.glslProgram.resources.filterIsInstance<ShaderFactory.GLSLUniformSampler2D>()
                .find { it.name == "virtualTextures" } ?: throw Exception("You need a virtualTextures uniform to use virtual texturing.")

        SLICE_SIZE = vtResource.count
        samplers = Array(SLICE_SIZE) { VulkanSampler(backend) }

        val framesInFlight = backend.swapchain.maxFramesInFlight

        val resourcesSize = VkDescriptorPoolSize.callocStack(1).apply {
            type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            descriptorCount(SLICE_SIZE * MAX_SLICES * framesInFlight)
        }

        val poolCreateInfo = VkDescriptorPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).apply {
            pPoolSizes(resourcesSize)
            maxSets(MAX_SLICES * framesInFlight)
        }

        val pDescriptorPool = stackMallocLong(1)
        vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pDescriptorPool).ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
        pool = pDescriptorPool.get(0)

        // Bulk-allocate sets for however many slices we allow
        val descriptorSetLayout = program.descriptorSetLayouts[0] // Descriptor set layout zero = layout of the set containing just vtResource
        val layouts = stackMallocLong(MAX_SLICES * framesInFlight)
        for (i in 0 until MAX_SLICES * framesInFlight) {
            layouts.put(descriptorSetLayout)
        }
        layouts.flip()

        val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
            descriptorPool(pool)
            pSetLayouts(layouts)
        }

        val pDescriptorSets = stackMallocLong(MAX_SLICES * framesInFlight)
        vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)
        sets = InflightFrameResource(backend) {
            val a = LongArray(MAX_SLICES)
            pDescriptorSets.get(a)
            a
        }

        stackPop()
    }

    fun begin(commandBuffer: VkCommandBuffer, pipeline: Pipeline, frame: Frame, callback: () -> Unit) =
            VirtualTexturingContext(commandBuffer, pipeline, frame, callback)

    inner class VirtualTexturingContext(val commandBuffer: VkCommandBuffer, val pipeline: Pipeline, val frame: Frame, val callback: () -> Unit) {
        var slice = 0
        val sliceContents = mutableMapOf<VulkanTexture2D, Int>()
        val reverseContents = mutableListOf<VulkanTexture2D>()
        var nextId = 0

        init {
            //vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, stackLongs(sets[slice]), null)
        }

        fun translate(texture: VulkanTexture2D): Int {
            if (sliceContents.size == SLICE_SIZE) {
                writeCurrentSlice()
                nextSlice()
            }

            val id = sliceContents[texture] ?: let {
                sliceContents[texture] = nextId
                reverseContents.add(texture)
                nextId++
            }

            return id
        }

        fun writeCurrentSlice() {
            if(sliceContents.size == 0)
                return

            //val imageInfo = VkDescriptorImageInfo.callocStack(reverseContents.size)
            val imageInfo = VkDescriptorImageInfo.callocStack(SLICE_SIZE)

            /*reverseContents.forEachIndexed { i, e ->
                imageInfo[i].apply {
                    imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?

                    imageView(e.imageView)
                    sampler(tempSampler.handle)
                }
            }*/

            //TODO preinitialize slices or validation will be unhappy
            for(i in 0 until SLICE_SIZE) {
                imageInfo[i].apply {
                    imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?

                    if(i < reverseContents.size)
                        imageView(reverseContents[i].imageView)
                    else
                        imageView(reverseContents[0].imageView)

                    sampler(samplers[i].handle)
                }
            }

            val destinationSet = sets[frame][slice]
            val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
                dstSet(destinationSet)
                dstBinding(0) // only one thing in this set, the virtualTextures[] doodad... or is it ?
                dstArrayElement(0)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)

                // Just update the descriptor for our lone ubo buffer
                pImageInfo(imageInfo)
            }

            vkUpdateDescriptorSets(backend.logicalDevice.vkDevice, stuffToWrite, null)

            //TODO only do this once I'm sure the next slice will be used
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, stackLongs(sets[frame][slice]), null)
            //println("writing to slice [$slice] ${reverseContents.size}")
        }

        fun nextSlice() {
            callback()

            slice++
            nextId = 0
            sliceContents.clear()
            reverseContents.clear()
        }

        fun finish() {
            writeCurrentSlice()
            nextSlice()
        }
    }

    override fun cleanup() {
        vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, pool, null)
        samplers.forEach(Cleanable::cleanup)
    }

    companion object {
        /** We'll clamp the amount of textures in a descriptor array to some sane number */
        const val MAX_VIRTUAL_TEXTURING_ARRAY_SIZE = 512

        /** 32kb buffers for virtual texturing ought to be enough for everyone right ? */
        const val MAX_SLICES = 32
        //const val VIRTUAL_TEXTURING_BUFFER_SIZE = 1024 * 1024 * 32
        //const val VIRTUAL_TEXTURING_BUFFER_ENTRIES = VIRTUAL_TEXTURING_BUFFER_SIZE / 4

        fun VulkanGraphicsBackend.getNumberOfSlotsForVirtualTexturing(resources: List<ShaderFactory.GLSLUniformResource>): Int {
            stackPush()

            val physicalDeviceProperties = VkPhysicalDeviceProperties.callocStack()
            vkGetPhysicalDeviceProperties(this.physicalDevice.vkPhysicalDevice, physicalDeviceProperties)

            val reservedForUBOs = resources.count { it is ShaderFactory.GLSLUniformBlock }
            val reservedForNonVirtualTextureInputs = resources.count { it is ShaderFactory.GLSLUniformSampler2D }

            println("${physicalDeviceProperties.deviceNameString()} : ${physicalDeviceProperties.limits().maxPerStageResources().safe()}")

            val limits = physicalDeviceProperties.limits()

            val possibleLimits = listOf(
                    // We can't have more combined samplers than we have for the individual resources
                    // Plus we need to reserve a bunch of them
                    limits.maxDescriptorSetSamplers().safe() - reservedForNonVirtualTextureInputs,
                    limits.maxDescriptorSetSampledImages().safe() - reservedForNonVirtualTextureInputs,
                    limits.maxPerStageDescriptorSamplers().safe() - reservedForNonVirtualTextureInputs,
                    limits.maxPerStageDescriptorSampledImages().safe() - reservedForNonVirtualTextureInputs,

                    /* This one is weirder: it is possible that the maximum number of bound resources is lower than the sum of
                     the limits of each individual resource type. In this case we need to ensure we still have enough descriptors
                     for UBOs and virtual texturing stuff  */
                    (limits.maxPerStageResources().safe() - reservedForUBOs - reservedForNonVirtualTextureInputs)
            )

            println(possibleLimits)

            val virtualTexturingSlots = Math.min(possibleLimits.min()!!, MAX_VIRTUAL_TEXTURING_ARRAY_SIZE)
            if (virtualTexturingSlots <= 0)
                throw Exception("Oops! This device doesn't have the ability to bind everything we need for this shader sadly !")

            stackPop()

            return virtualTexturingSlots
        }
    }
}

private fun Int.safe(): Int = if (this == -1) Int.MAX_VALUE else this
