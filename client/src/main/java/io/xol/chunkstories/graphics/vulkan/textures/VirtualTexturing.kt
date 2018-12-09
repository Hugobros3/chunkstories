package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.api.graphics.Texture2D
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.resources.InflightFrameResource
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorPool
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorSet
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.util.concurrent.ConcurrentLinkedDeque

class VirtualTexturing(val backend: VulkanGraphicsBackend) : Cleanable {

    private val contextCapacity: Int
    private val setLayout : VkDescriptorSetLayout

    private val sampler = VulkanSampler(backend)

    private val pools = mutableListOf<VkDescriptorPool>()
    private val deque = ConcurrentLinkedDeque<VirtualTexturingContext>()

    companion object {
        /** We'll clamp the amount of textures in a descriptor array to some sane number */
        const val MAX_VIRTUAL_TEXTURING_ARRAY_SIZE = 1024
    }

    init {
        stackPush()

        contextCapacity = decideContextCapacity()
        setLayout = createSetLayout()

        prepareContexts(64)

        stackPop()
    }

    private fun decideContextCapacity() : Int {
        val physicalDeviceProperties = VkPhysicalDeviceProperties.callocStack()
        vkGetPhysicalDeviceProperties(backend.physicalDevice.vkPhysicalDevice, physicalDeviceProperties)

        val limits = physicalDeviceProperties.limits()

        fun Int.safe(): Int = if (this == -1) Int.MAX_VALUE else this

        val reservedSamplers = 32
        val reservedOtherRessourcesSlots = 32

        val possibleLimits = listOf(
                // We can't have more combined samplers than we have for the individual resources
                // Plus we need to reserve a bunch of them
                limits.maxDescriptorSetSamplers().safe() - reservedSamplers,
                limits.maxDescriptorSetSampledImages().safe() - reservedSamplers,
                limits.maxPerStageDescriptorSamplers().safe() - reservedSamplers,
                limits.maxPerStageDescriptorSampledImages().safe() - reservedSamplers,

                /* This one is weirder: it is possible that the maximum number of bound resources is lower than the sum of
                 the limits of each individual resource type. In this case we need to ensure we still have enough descriptors
                 for UBOs and virtual texturing stuff  */
                (limits.maxPerStageResources().safe() - reservedOtherRessourcesSlots - reservedSamplers)
        )

        return Math.min(possibleLimits.min()!!, MAX_VIRTUAL_TEXTURING_ARRAY_SIZE)
    }

    private fun createSetLayout() : VkDescriptorSetLayout {
        stackPush()

        val createInfo  = VkDescriptorSetLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).apply {
            val bindings = VkDescriptorSetLayoutBinding.callocStack(1).apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                descriptorCount(contextCapacity)
                stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS)
            }

            pBindings(bindings)
        }

        val pDescriptorSetLayout = stackLongs(0)
        vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, createInfo, null, pDescriptorSetLayout)

        val layout = pDescriptorSetLayout.get(0)

        stackPop()

        return layout
    }

    fun prepareContexts(count: Int) {
        stackPush()

        println("preparing $count contexts")

        val resourcesSize = VkDescriptorPoolSize.callocStack(1).apply {
            type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            descriptorCount(contextCapacity * count)
        }

        val poolCreateInfo = VkDescriptorPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).apply {
            pPoolSizes(resourcesSize)
            maxSets(count)
        }

        val pPool = stackMallocLong(1)
        vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pPool).ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
        val pool = pPool.get(0)

        val layouts = stackMallocLong(count)
        for (i in 0 until count) {
            layouts.put(setLayout)
        }
        layouts.flip()

        val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
            descriptorPool(pool)
            pSetLayouts(layouts)
        }

        val pDescriptorSets = stackMallocLong(count)
        vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)

        for(i in 0 until count) {
            val set = pDescriptorSets.get()
            deque.add(VirtualTexturingContext(set))
        }

        pools.add(pool)

        stackPop()
    }

    fun getVirtualTexturingContext() : VirtualTexturingContext {
        while(true) {
            val context = deque.poll()
            if(context != null)
                return context
            else
                prepareContexts(16)
        }
    }

    inner class VirtualTexturingContext internal constructor(val setHandle: VkDescriptorSet) {
        val ids = mutableMapOf<VulkanTexture2D, Int>()
        val content = mutableListOf<VulkanTexture2D>()

        fun translate(texture: Texture2D) : TranslationResult {
            val texture: VulkanTexture2D = texture as VulkanTexture2D

            val id = ids[texture]
            return when {
                id != null -> TranslationResult.Success(id)
                else -> when {
                    content.size >= contextCapacity -> TranslationResult.Full
                    else -> {
                        val id = content.size
                        content.add(texture)
                        ids[texture] = id
                        TranslationResult.Success(id)
                    }
                }
            }
        }

        private fun reset() {
            ids.clear()
            content.clear()
        }

        fun updateContents() {
            if(content.size == 0)
                return

            val imageInfo = VkDescriptorImageInfo.callocStack(contextCapacity)

            for(i in 0 until contextCapacity) {
                imageInfo[i].apply {
                    imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?

                    if(i < content.size)
                        imageView(content[i].imageView)
                    else
                        imageView(content[0].imageView)

                    sampler(sampler.handle)
                }
            }

            val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
                dstSet(setHandle)
                dstBinding(0) // only one thing in this set, the virtualTextures[] doodad... or is it ?
                dstArrayElement(0)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                // Just update the descriptor for our lone ubo buffer
                pImageInfo(imageInfo)
            }

            vkUpdateDescriptorSets(backend.logicalDevice.vkDevice, stuffToWrite, null)
        }

        fun returnToPool() {
            reset()
            deque.add(this)
        }
    }

    sealed class TranslationResult {
        class Success(val id: Int) : TranslationResult()
        object Full : TranslationResult()
    }

    override fun cleanup() {
        for(pool in pools) {
            vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, pool, null)
        }
        vkDestroyDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayout, null)
    }
}