package xyz.chunkstories.graphics.vulkan.buffers

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.JvmStructField
import java.nio.ByteBuffer

class VulkanUniformBuffer(backend: VulkanGraphicsBackend, val mapper: GLSLType.JvmStruct) :
        VulkanBuffer(backend, mapper.size.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, true) {

    fun upload(interfaceBlock: InterfaceBlock) {
        stackPush()

        val fillMe = stackMalloc(mapper.size)

        for (field in mapper.fields) {
            fillMe.position(field.offset)
            //println("${field.name} ${fillMe.position()}")
            extractInterfaceBlockField(field, fillMe, interfaceBlock)
        }

        fillMe.position(0)
        fillMe.limit(fillMe.capacity())
        upload(fillMe)

        stackPop()
    }

    companion object {
        val zero2 = Vector2f(0.0F)
        val zero3 = Vector3f(0.0F)
        val zero4 = Vector4f(0.0F)

        val zero2d = Vector2d(0.0)
        val zero3d = Vector3d(0.0)
        val zero4d = Vector4d(0.0)

        val zero2i = Vector2i(0)
        val zero3i = Vector3i(0)
        val zero4i = Vector4i(0)

        val mat4identity = Matrix4f()
        val mat3identity = Matrix3f()
    }
}