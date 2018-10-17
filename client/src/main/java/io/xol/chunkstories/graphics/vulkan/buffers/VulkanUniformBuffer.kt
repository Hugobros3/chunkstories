package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.graphics.common.shaderc.InterfaceBlockGLSLMapping
import io.xol.chunkstories.graphics.common.shaderc.GLSLBaseType
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*

class VulkanUniformBuffer(backend: VulkanGraphicsBackend, val mapper: InterfaceBlockGLSLMapping) :
        VulkanBuffer(backend, mapper.size.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT) {

    fun upload(interfaceBlock: InterfaceBlock) {
        stackPush()

        val fillMe = stackMalloc(mapper.size)

        for(field in mapper.fields) {
            fillMe.position(field.offset)

            when(field.type) {
                GLSLBaseType.FLOAT -> fillMe.putFloat(field.property.getter.call(interfaceBlock) as Float)
                GLSLBaseType.INT -> fillMe.putInt(field.property.getter.call(interfaceBlock) as Int)
                GLSLBaseType.LONG -> fillMe.putInt((field.property.getter.call(interfaceBlock) as Long).toInt())

                GLSLBaseType.VEC2 -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector2f ?: zero2
                    fillMe.putFloat(vec.x)
                    fillMe.putFloat(vec.y)
                }

                GLSLBaseType.VEC3 -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector3f ?: zero3
                    fillMe.putFloat(vec.x)
                    fillMe.putFloat(vec.y)
                    fillMe.putFloat(vec.z)
                }

                GLSLBaseType.VEC4 -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector4f ?: zero4
                    fillMe.putFloat(vec.x)
                    fillMe.putFloat(vec.y)
                    fillMe.putFloat(vec.z)
                    fillMe.putFloat(vec.w)
                }

                else -> throw Exception("field type ${field.type} does not have a byteBuffer-filling branch")
            }
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
    }
}