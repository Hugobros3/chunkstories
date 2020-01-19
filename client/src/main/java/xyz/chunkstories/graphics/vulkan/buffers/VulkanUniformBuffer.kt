package xyz.chunkstories.graphics.vulkan.buffers

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.util.extractInterfaceBlock
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern

class VulkanUniformBuffer(backend: VulkanGraphicsBackend, val mapper: GLSLType.JvmStruct) : VulkanBuffer(backend, mapper.size.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, MemoryUsagePattern.DYNAMIC) {

    fun upload(interfaceBlock: InterfaceBlock) {
        stackPush()

        val fillMe = stackMalloc(mapper.size)

        extractInterfaceBlock(fillMe, 0, interfaceBlock, mapper)

        fillMe.position(0)
        fillMe.limit(fillMe.capacity())
        upload(fillMe)

        stackPop()
    }
}