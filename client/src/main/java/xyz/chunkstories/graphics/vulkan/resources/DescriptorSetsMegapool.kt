package xyz.chunkstories.graphics.vulkan.resources

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.*
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.shaders.DescriptorSlotLayout
import xyz.chunkstories.graphics.vulkan.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DescriptorSetsMegapool(val backend: VulkanGraphicsBackend) : Cleanable {
    private val layoutsLock = ReentrantLock()

    private val layouts = mutableMapOf<Set<GLSLResource>, ReferenceCountedDescriptorSlotLayout>()
    private val poolsForLayouts = mutableMapOf<DescriptorSlotLayout, LayoutSubpool>()

    val samplers = VulkanSamplers(backend)

    internal fun getSubpoolForLayout(layout: DescriptorSlotLayout): LayoutSubpool = poolsForLayouts[layout] ?: throw Exception("This layout no longer exists! Did you get it from a destroyed shader too ?")

    /** A pool of descriptor pools contaning only instances of a single descriptor set layout. The descriptor sets get recycled. */
    inner class LayoutSubpool internal constructor(val layout: DescriptorSlotLayout) : Cleanable {
        var allocationSize = 4
        var allocatedTotal = 0
        val pools = mutableListOf<VkDescriptorPool>()

        val available = ConcurrentLinkedDeque<VkDescriptorSet>()

        private fun createMoreDescriptorSets() {
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
                layouts.put(layout.vkLayoutHandle)
            }
            layouts.flip()

            val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
                descriptorPool(newDescriptorPool)
                pSetLayouts(layouts)
            }

            val pDescriptorSets = memAllocLong(descriptorSetsCount)
            vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)

            memFree(layouts)

            val instances = LongArray(allocationSize)
            pDescriptorSets.get(instances)
            memFree(pDescriptorSets)
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

    data class ReferenceCountedDescriptorSlotLayout(var users: Int = 0, val descriptorSlotLayout: DescriptorSlotLayout)

    fun acquireDescriptorSlotLayout(slotResources: Set<GLSLResource>): DescriptorSlotLayout {
        layoutsLock.withLock {
            val entry = layouts.getOrPut(slotResources) {
                val descriptorSetLayout = DescriptorSlotLayout(backend, slotResources)

                if(poolsForLayouts.containsKey(descriptorSetLayout))
                    throw Exception("Layout subpools should *not* get duplicated !")

                poolsForLayouts[descriptorSetLayout] = LayoutSubpool(descriptorSetLayout)
                ReferenceCountedDescriptorSlotLayout(descriptorSlotLayout = descriptorSetLayout)
            }
            entry.users++
            return entry.descriptorSlotLayout
        }
    }

    fun releaseDescriptorSlotLayout(descriptorSlotLayout: DescriptorSlotLayout) {
        layoutsLock.withLock {
            val entry = layouts[descriptorSlotLayout.resources] ?: throw Exception("Double free")
            entry.users--
            if (entry.users == 0) {
                poolsForLayouts[descriptorSlotLayout]!!.cleanup()
                poolsForLayouts.remove(descriptorSlotLayout)

                entry.descriptorSlotLayout.cleanup()
                layouts.remove(descriptorSlotLayout.resources)
            }
        }
    }

    override fun cleanup() {
        //mainPool.values.forEach { it.cleanup() }
        samplers.cleanup()
    }
}