package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.ByteBuffer

class ShaderModule(val backend: VulkanGraphicsBackend, val spirv : ByteBuffer) {
    constructor(backend: VulkanGraphicsBackend, inputStream: InputStream) : this(backend, inputStream.toByteBuffer())

    val handle: VkShaderModule

    init {
        stackPush()
        val createInfo = VkShaderModuleCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).apply {
            pCode(spirv)
        }

        val pShaderModule = stackMallocLong(1)
        vkCreateShaderModule(backend.logicalDevice.vkDevice, createInfo, null, pShaderModule)

        handle = pShaderModule.get(0)
        stackPop()
    }

    fun cleanup() {
        vkDestroyShaderModule(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}