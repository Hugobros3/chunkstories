package xyz.chunkstories.graphics.vulkan.util

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.buffers.VulkanUniformBuffer
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture

fun VulkanGraphicsBackend.writeUniformBufferDescriptor(set: VkDescriptorSet, binding: Int, buffer: VulkanBuffer, offset: Long, range: Long) {
    stackPush()

    val bufferInfo = VkDescriptorBufferInfo.callocStack(1).also {
        it.buffer(buffer.handle)
        it.offset(offset)
        it.range(range)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).also {
        it.dstSet(set)
        it.dstBinding(binding)
        it.dstArrayElement(0)
        it.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

        // Just update the descriptor for our lone ubo buffer
        it.pBufferInfo(bufferInfo)
        it.descriptorCount(1)
    }

    //println("offset: $offset range: $range")
    if(range == 0L)
        throw Exception("RIP")

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeStorageBufferDescriptor(set: VkDescriptorSet, binding: Int, buffer: VulkanBuffer, offset: Long = 0) {
    stackPush()

    val bufferInfo = VkDescriptorBufferInfo.callocStack(1).also {
        it.buffer(buffer.handle)
        it.offset(offset)
        it.range(buffer.bufferSize)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).also {
        it.dstSet(set)
        it.dstBinding(binding)
        it.dstArrayElement(0)
        it.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)

        // Just update the descriptor for our lone ubo buffer
        it.pBufferInfo(bufferInfo)
        it.descriptorCount(1)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeCombinedImageSamplerDescriptor(set: VkDescriptorSet, binding: Int, texture: VulkanTexture, sampler: VulkanSampler, dstArrayElement: Int = 0) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).also {
        it.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
        it.imageView(texture.imageView)
        it.sampler(sampler.handle)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).also {
        it.dstSet(set)
        it.dstBinding(binding)
        it.dstArrayElement(dstArrayElement)
        it.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)

        // Just update the descriptor for our lone ubo buffer
        it.pImageInfo(imageInfo)
        it.descriptorCount(1)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeSampledImageDescriptor(set: VkDescriptorSet, binding: Int, texture: VulkanTexture, dstArrayElement: Int = 0) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).also {
        it.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
        it.imageView(texture.imageView)
        it.sampler(VK_NULL_HANDLE)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).also {
        it.dstSet(set)
        it.dstBinding(binding)
        it.dstArrayElement(dstArrayElement)
        it.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)

        it.pImageInfo(imageInfo)
        it.descriptorCount(1)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeSamplerDescriptor(set: VkDescriptorSet, binding: Int, sampler: VulkanSampler) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
        it.imageView(VK_NULL_HANDLE)
        it.sampler(sampler.handle)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        it.dstSet(set)
        it.dstBinding(binding)
        it.dstArrayElement(0)
        it.descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)

        it.pImageInfo(imageInfo)
        it.descriptorCount(1)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.copyDescriptorSet(source: VkDescriptorSet, destination: VkDescriptorSet, setSize: Int) {
    stackPush()

    val copies = VkCopyDescriptorSet.callocStack(1).let {
        it.srcSet(source)
        it.dstSet(destination)
        it.descriptorCount(setSize)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, null, copies)

    stackPop()
}
