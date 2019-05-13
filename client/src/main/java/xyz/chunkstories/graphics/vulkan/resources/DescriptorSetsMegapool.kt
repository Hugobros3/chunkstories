package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanUniformBuffer
import xyz.chunkstories.graphics.vulkan.shaders.DescriptorSlotLayout
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.vulkan.textures.*
import xyz.chunkstories.graphics.vulkan.util.*
import java.nio.IntBuffer
import java.util.concurrent.ConcurrentLinkedDeque

class DescriptorSetsMegapool(val backend: VulkanGraphicsBackend) : Cleanable {

    private val mainPool = mutableMapOf<DescriptorSlotLayout, LayoutSubpool>()

    val samplers = VulkanSamplers(backend)

    private fun getSubpoolForLayout(layout: DescriptorSlotLayout): LayoutSubpool = mainPool.getOrPut(layout) {
        LayoutSubpool(layout)
    }

    /** A pool of descriptor pools contaning only instances of a single descriptor set type. The descriptor sets get recycled. */
    inner class LayoutSubpool internal constructor(val layout: DescriptorSlotLayout) : Cleanable {
        var allocationSize = 4
        var allocatedTotal = 0
        val pools = mutableListOf<VkDescriptorPool>()

        val available = ConcurrentLinkedDeque<VkDescriptorSet>()

        init {
            if(layout.variableSize)
                TODO("Handle variable size layouts")
        }

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

            val pDescriptorPool = stackMallocLong(1)
            vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pDescriptorPool)
                    .ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
            val newDescriptorPool = pDescriptorPool.get(0)
            pools.add(newDescriptorPool)

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

            //println("Created $allocationSize new descriptors for layout $layout")

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

        val samplers: VulkanSamplers
            get() = this@DescriptorSetsMegapool.samplers

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
                TODO("kill")
                //backend.copyDescriptorSet(oldset, set, slotLayout.bindingsCountTotal)

                spentSets.getOrPut(subpool) { mutableListOf() }.add(oldset)
            }

            return set
        }

        fun bindUBO(instanceName: String, interfaceBlock: InterfaceBlock) {
            //TODO path w/ name instead of IB class
            val uboBindPoint = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformBlock>().find {
                it.struct.kClass == interfaceBlock.javaClass.kotlin //TODO ::class ?
                && it.name == instanceName
            } ?: throw Exception("I can't find a program resource matching that interface block :s")

            val set = getSet(uboBindPoint.descriptorSetSlot)

            //TODO UBO MEGAPOOL
            val buffer = VulkanUniformBuffer(backend, uboBindPoint.struct)
            buffer.upload(interfaceBlock)
            backend.updateDescriptorSet(set, uboBindPoint.binding, buffer)
            tempBuffers.add(buffer)
        }

        fun bindSSBO(name: String, buffer: VulkanBuffer, offset: Long = 0) {
            val ssboBindPoint = pipeline.program.glslProgram.instancedInputs.find { it.name == name }!!.shaderStorage

            val set = getSet(ssboBindPoint.descriptorSetSlot)

            backend.updateDescriptorSet(set, ssboBindPoint.binding, buffer, offset)
        }

        fun bindTextureAndSampler(name: String, texture: VulkanTexture2D, sampler: VulkanSampler, index: Int = 0) {
            val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage2D>().find {
                it.name == name
            } ?: return // ?: throw Exception("I can't find a program sampler2D resource matching that name '$name'")

            val set = getSet(resource.descriptorSetSlot)
            backend.updateDescriptorSet(set, resource.binding, texture, sampler, index)
        }

        fun bindTextureAndSampler(name: String, texture: VulkanOnionTexture2D, sampler: VulkanSampler, index: Int = 0) {
            val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage2DArray>().find {
                it.name == name
            } ?: return // ?: throw Exception("I can't find a program sampler2DArray resource matching that name '$name'")

            val set = getSet(resource.descriptorSetSlot)
            backend.updateDescriptorSet(set, resource.binding, texture, sampler, index)
        }

        fun bindTextureAndSampler(name: String, texture: VulkanTexture3D, sampler: VulkanSampler, index: Int = 0) {
            val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImage3D>().find {
                it.name == name
            } ?: return // ?: throw Exception("I can't find a program sampler3D resource matching that name '$name'")

            val set = getSet(resource.descriptorSetSlot)
            backend.updateDescriptorSet(set, resource.binding, texture, sampler, index)
        }

        fun bindTextureAndSampler(name: String, texture: VulkanTextureCubemap, sampler: VulkanSampler, index: Int = 0) {
            val resource = pipeline.program.glslProgram.resources.filterIsInstance<GLSLUniformSampledImageCubemap>().find {
                it.name == name
            } ?: return // ?: throw Exception("I can't find a program sampler3D resource matching that name '$name'")

            val set = getSet(resource.descriptorSetSlot)
            backend.updateDescriptorSet(set, resource.binding, texture, sampler, index)
        }

        fun preDraw(commandBuffer: VkCommandBuffer) {
            stackPush()
            for ((slot, set) in sets.entries) {
                vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipelineLayout, slot, stackLongs(set), null as? IntBuffer)
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
        samplers.cleanup()
    }
}