package xyz.chunkstories.graphics.vulkan.textures

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDescriptorIndexing.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.util.*

/**
 * Class for handling set 0 as a mega-bind (or bindless) point for all the textures loaded in-engine.
 * Makes good use of EXT_Descriptor_Indexing
 */
class GlobalTextures(val backend: VulkanGraphicsBackend) : Cleanable {

    private val setLayout : VkDescriptorSetLayout
    val sampler = VulkanSampler(backend)

    private val pool: VkDescriptorPool
    val theSet : VkDescriptorSet

    private val mappings: MutableMap<VulkanTexture2D, Int> = mutableMapOf()

    init {
        setLayout = createSetLayout()
        pool = createPool()
        theSet = createSet()

        backend.updateDescriptorSet(theSet, 0, sampler)
    }

    private fun createSetLayout() : VkDescriptorSetLayout {
        MemoryStack.stackPush()

        val createInfo  = VkDescriptorSetLayoutCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).apply {
            val bindings = VkDescriptorSetLayoutBinding.callocStack(2)

            bindings[0].apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
                descriptorCount(1)
                stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS)
            }

            bindings[1].apply {
                binding(1)
                descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                descriptorCount(magicTexturesUpperBound)
                stageFlags(VK_SHADER_STAGE_ALL_GRAPHICS)
            }

            pBindings(bindings)

            flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT_EXT)
        }

        val bindingFlags = stackCallocInt(2)
        bindingFlags.put(0, 0)
        bindingFlags.put(1,
                        VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT_EXT or
                        VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT_EXT or
                        VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT_EXT
        )

        val bindingFlagsCreateInfo = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.callocStack().apply {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO_EXT)

            bindingCount(2)
            pBindingFlags(bindingFlags)
            pNext(createInfo.pNext())
        }

        createInfo.pNext(bindingFlagsCreateInfo.address())

        val pDescriptorSetLayout = MemoryStack.stackLongs(0)
        vkCreateDescriptorSetLayout(backend.logicalDevice.vkDevice, createInfo, null, pDescriptorSetLayout).ensureIs("Failed to create magic descriptor set layout", VK_SUCCESS)

        val layout = pDescriptorSetLayout.get(0)

        MemoryStack.stackPop()

        return layout
    }

    private fun createPool() : VkDescriptorPool {
        stackPush().use {
            val resourcesSize = VkDescriptorPoolSize.callocStack(2)
            resourcesSize[0].apply {
                type(VK_DESCRIPTOR_TYPE_SAMPLER)
                descriptorCount(1)
            }
            resourcesSize[1].apply {
                type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                descriptorCount(magicTexturesUpperBound)
            }
            resourcesSize.position(0)
            resourcesSize.limit(2)

            val poolCreateInfo = VkDescriptorPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).apply {
                pPoolSizes(resourcesSize)
                maxSets(1)
                flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT)
            }

            val pPool = MemoryStack.stackMallocLong(1)
            vkCreateDescriptorPool(backend.logicalDevice.vkDevice, poolCreateInfo, null, pPool).ensureIs("Failed to create descriptor pool !", VK_SUCCESS)
            val pool = pPool.get(0)

            return pool
        }
    }

    private fun createSet() : VkDescriptorSet {
        stackPush().use {
            val layouts = MemoryStack.stackMallocLong(1)
            layouts.put(setLayout)
            layouts.flip()

            val allocInfo = VkDescriptorSetAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).apply {
                descriptorPool(pool)
                pSetLayouts(layouts)
            }

            val allocateInfoVL = VkDescriptorSetVariableDescriptorCountAllocateInfoEXT.callocStack().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO_EXT).apply {
                pDescriptorCounts(stackInts(2048))
            }

            allocInfo.pNext(allocateInfoVL.address())

            val pDescriptorSets = MemoryStack.stackMallocLong(1)
            vkAllocateDescriptorSets(backend.logicalDevice.vkDevice, allocInfo, pDescriptorSets).ensureIs("Failed to allocate descriptor sets :( ", VK_SUCCESS)
            return pDescriptorSets.get(0)
        }
    }

    fun assignId(vulkanTexture2D: VulkanTexture2D) : Int {
        val id = mappings.size
        mappings[vulkanTexture2D] = id
        backend.updateDescriptorSet(theSet, 1, vulkanTexture2D, id)

        //Thread.dumpStack()
        //println("$vulkanTexture2D")

        if(!prepared) {
            for(i in 0 until 2048) {
                backend.updateDescriptorSet(theSet, 1, vulkanTexture2D, i)
            }
            prepared = true
        }

        return id
    }

    var prepared = false

    companion object {
        val magicTexturesUpperBound = 65536
        val magicTexturesNames = setOf("textures2D")
    }

    override fun cleanup() {
        sampler.cleanup()

        vkDestroyDescriptorSetLayout(backend.logicalDevice.vkDevice, setLayout, null)
        vkDestroyDescriptorPool(backend.logicalDevice.vkDevice, pool, null)
    }
}