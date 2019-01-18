package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanUniformBuffer
import xyz.chunkstories.graphics.vulkan.shaders.DescriptorSlotLayout
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import xyz.chunkstories.graphics.vulkan.util.VkDescriptorPool
import xyz.chunkstories.graphics.vulkan.util.VkDescriptorSet
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.common.shaders.GLSLUniformBlock
import xyz.chunkstories.graphics.common.shaders.GLSLUniformSampler2D
import java.nio.IntBuffer
import java.util.concurrent.ConcurrentLinkedDeque

class DescriptorSetsMegapool(val backend: VulkanGraphicsBackend) : Cleanable {

    private val mainPool = mutableMapOf<DescriptorSlotLayout, LayoutSubpool>()

    private fun getSubpoolForLayout(layout: DescriptorSlotLayout): LayoutSubpool = mainPool.getOrPut(layout) {
        LayoutSubpool(layout)
    }

    /** A pool of descriptor pools contaning only instances of a single descriptor set type. The descriptor sets get recycled. */
    inner class LayoutSubpool internal constructor(val layout: DescriptorSlotLayout) : Cleanable {
        var allocationSize = 4
        var allocatedTotal = 0
        val pools = mutableListOf<VkDescriptorPool>()

        val available = ConcurrentLinkedDeque<VkDescriptorSet>()

        fun createMoreDescriptorSets() {
            stackPush()

            val descriptorsCountPerType = layout.descriptorsCountByType

            val resourcesSize = VkDescriptorPoolSize.callocStack(descriptorsCountPerType.keys.size)
            descriptorsCountPerType.entries.forEachIndexed { index, (descriptorType, count) ->
                resourcesSize[index].apply {
                    type(descriptorType)
                    descriptorCount(count * allocationSize)
                }
            }

            val descriptorSetsCount = allocationSize

            // Create the pool at last
            val poolCreateInfo = VkDescriptorPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).apply {
                pPoolSizes(resourcesSize)
                maxSets(descriptorSetsCount)
            }

            println(layout)

            val pDescriptorPool = stackMallocLong(1)
            vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pDescriptorPool)
                    .ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
            val newDescriptorPool = pDescriptorPool.get(0)

            // Allocate every set we need from this pool
            val layouts = memAllocLong(descriptorSetsCount) // allocate on heap because this gets big
            for (i in 0 until allocationSize) {
                layouts.put(layout.vulkanLayout)
            }
            layouts.flip()

            val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
                descriptorPool(newDescriptorPool)
                pSetLayouts(layouts)
            }

            val pDescriptorSets = stackMallocLong(descriptorSetsCount)
            vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)

            memFree(layouts)

            val instances = LongArray(allocationSize)
            pDescriptorSets.get(instances)
            available.addAll(instances.asList())

            println("Created $allocationSize new descriptors for layout $layout")

            // Geometric growth for our descriptor sets pools
            allocatedTotal += allocationSize
            allocationSize *= 2

            stackPop()
        }

        fun acquireDescriptorSet(): VkDescriptorSet {
            if (available.size == 0)
                createMoreDescriptorSets()
            return available.removeLast()
        }

        override fun cleanup() {
            pools.forEach { vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, it, null) }
        }
    }

    fun getBindingContext(pipeline: Pipeline) = ShaderBindingContext(pipeline)

    /** Thread UNSAFE semi-immediate mode emulation of the conventional binding model */
    inner class ShaderBindingContext internal constructor(val pipeline: Pipeline) {
        val sets = mutableMapOf<Int, VkDescriptorSet>()
        val dirty = mutableSetOf<Int>()

        val spentSets = mutableMapOf<LayoutSubpool, MutableList<VkDescriptorSet>>()
        val tempBuffers = mutableListOf<VulkanBuffer>()

        private fun getSet(slot: Int): VkDescriptorSet {
            val slotLayout = pipeline.program.slotLayouts[slot]
            val subpool = getSubpoolForLayout(slotLayout)
            var set = sets[slot]

            if (set == null) {
                set = subpool.acquireDescriptorSet()
                sets.put(slot, set)
            } else if (dirty.remove(slot)) {
                val oldset = set
                set = subpool.acquireDescriptorSet()
                sets.put(slot, set)

                //TODO don't do this idk
                backend.copyDescriptorSet(oldset, set, slotLayout.bindingsCountTotal)

                spentSets.getOrPut(subpool) { mutableListOf() }.add(oldset)
            }

            return set
        }

        fun bindUBO(interfaceBlock: InterfaceBlock) {

            //TODO path w/ name instead of IB class
            val uboBindPoint = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformBlock>().find {
                it.struct.kClass == interfaceBlock.javaClass.kotlin //TODO ::class ?
            } ?: throw Exception("I can't find a program resource matching that interface block :s")

            val set = getSet(uboBindPoint.descriptorSetSlot)

            //TODO UBO MEGAPOOL
            val buffer = VulkanUniformBuffer(backend, uboBindPoint.struct)
            buffer.upload(interfaceBlock)
            backend.updateDescriptorSet(set, uboBindPoint.binding, buffer)
            tempBuffers.add(buffer)
        }

        fun bindTextureAndSampler(name: String, texture: VulkanTexture2D, sampler: VulkanSampler) {
            val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampler2D>().find {
                it.name == name
            } ?: throw Exception("I can't find a program sampler2D resource matching that name '$name' :s")

            val set = getSet(resource.descriptorSetSlot)
            backend.updateDescriptorSet(set, resource.binding, texture, sampler)
        }

        fun preDraw(commandBuffer: VkCommandBuffer) {
            stackPush()
            for ((slot, set) in sets.entries) {
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, slot, stackLongs(set), null as? IntBuffer)
            }

            dirty.addAll(sets.keys)
            stackPop()
        }

        fun recycle() {
            var i = 0
            for ((subpool, usedSlots) in spentSets) {
                subpool.available.addAll(usedSlots)
                i += usedSlots.size
            }

            for ((slot, set) in sets) {
                val slotLayout = pipeline.program.slotLayouts[slot]
                val subpool = getSubpoolForLayout(slotLayout)
                subpool.available.add(set)
                i++
            }

            //println("Recycled $i sets")

            for(buffer in tempBuffers)
                buffer.cleanup()
        }
    }

    override fun cleanup() {
        mainPool.values.forEach { it.cleanup() }
    }
}

private fun VulkanGraphicsBackend.updateDescriptorSet(set: VkDescriptorSet, binding: Int, buffer: VulkanUniformBuffer) {
    stackPush()

    val bufferInfo = VkDescriptorBufferInfo.callocStack(1).apply {
        buffer(buffer.handle)
        offset(0)
        range(VK_WHOLE_SIZE)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(0)
        descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

        // Just update the descriptor for our lone ubo buffer
        pBufferInfo(bufferInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

private fun VulkanGraphicsBackend.updateDescriptorSet(set: VkDescriptorSet, binding: Int, texture: VulkanTexture2D, sampler: VulkanSampler) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
        imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
        imageView(texture.imageView)
        sampler(sampler.handle)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(0)
        descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)

        // Just update the descriptor for our lone ubo buffer
        pImageInfo(imageInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

private fun VulkanGraphicsBackend.copyDescriptorSet(source: VkDescriptorSet, destination: VkDescriptorSet, setSize: Int) {
    stackPush()

    val copies = VkCopyDescriptorSet.callocStack(1).apply {
        srcSet(source)
        dstSet(destination)
        descriptorCount(setSize)

        this.descriptorCount()
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, null, copies)

    stackPop()
}
