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

fun VulkanGraphicsBackend.writeUniformBufferDescriptor(set: VkDescriptorSet, binding: Int, buffer: VulkanUniformBuffer, offset: Long, range: Long) {
    stackPush()

    val bufferInfo = VkDescriptorBufferInfo.callocStack(1).apply {
        buffer(buffer.handle)
        offset(offset)
        range(range)
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

fun VulkanGraphicsBackend.writeStorageBufferDescriptor(set: VkDescriptorSet, binding: Int, buffer: VulkanBuffer, offset: Long = 0) {
    stackPush()

    val bufferInfo = VkDescriptorBufferInfo.callocStack(1).apply {
        buffer(buffer.handle)
        offset(offset)
        range(buffer.bufferSize)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(0)
        descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)

        // Just update the descriptor for our lone ubo buffer
        pBufferInfo(bufferInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeCombinedImageSamplerDescriptor(set: VkDescriptorSet, binding: Int, texture: VulkanTexture, sampler: VulkanSampler, dstArrayElement: Int = 0) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
        imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
        imageView(texture.imageView)
        sampler(sampler.handle)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(dstArrayElement)
        descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)

        // Just update the descriptor for our lone ubo buffer
        pImageInfo(imageInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeSampledImageDescriptor(set: VkDescriptorSet, binding: Int, texture: VulkanTexture, dstArrayElement: Int = 0) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
        imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) //TODO maybe we can get that from VulkanTexture2D current layout field ?
        imageView(texture.imageView)
        sampler(VK_NULL_HANDLE)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(dstArrayElement)
        descriptorType(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)

        pImageInfo(imageInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.writeSamplerDescriptor(set: VkDescriptorSet, binding: Int, sampler: VulkanSampler) {
    stackPush()

    val imageInfo = VkDescriptorImageInfo.callocStack(1).apply {
        imageView(VK_NULL_HANDLE)
        sampler(sampler.handle)
    }

    val stuffToWrite = VkWriteDescriptorSet.callocStack(1).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).apply {
        dstSet(set)
        dstBinding(binding)
        dstArrayElement(0)
        descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)

        pImageInfo(imageInfo)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, stuffToWrite, null)

    stackPop()
}

fun VulkanGraphicsBackend.copyDescriptorSet(source: VkDescriptorSet, destination: VkDescriptorSet, setSize: Int) {
    stackPush()

    val copies = VkCopyDescriptorSet.callocStack(1).apply {
        srcSet(source)
        dstSet(destination)
        descriptorCount(setSize)
    }

    vkUpdateDescriptorSets(logicalDevice.vkDevice, null, copies)

    stackPop()
}
