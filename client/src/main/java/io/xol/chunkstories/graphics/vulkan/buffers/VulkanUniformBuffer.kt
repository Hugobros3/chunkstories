package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.graphics.common.shaderc.InterfaceBlockGLSLMapping
import io.xol.chunkstories.graphics.common.shaderc.GLSLBaseType
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*

class VulkanUniformBuffer(backend: VulkanGraphicsBackend, val mapper: InterfaceBlockGLSLMapping) :
        VulkanBuffer(backend, mapper.size.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, true) {

    fun upload(interfaceBlock: InterfaceBlock) {
        stackPush()

        val fillMe = stackMalloc(mapper.size)

        for(field in mapper.fields) {
            fillMe.position(field.offset)

            when(field.type) {
                GLSLBaseType.FLOAT.fieldType -> fillMe.putFloat(field.property.getter.call(interfaceBlock) as Float)
                GLSLBaseType.INT.fieldType -> fillMe.putInt(field.property.getter.call(interfaceBlock) as Int)
                GLSLBaseType.UINT.fieldType -> fillMe.putInt(field.property.getter.call(interfaceBlock) as Int)
                GLSLBaseType.LONG.fieldType -> fillMe.putInt((field.property.getter.call(interfaceBlock) as Long).toInt())

                GLSLBaseType.VEC2.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector2fc ?: zero2
                    fillMe.putFloat(vec.x())
                    fillMe.putFloat(vec.y())
                }

                GLSLBaseType.VEC3.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector3fc ?: zero3
                    fillMe.putFloat(vec.x())
                    fillMe.putFloat(vec.y())
                    fillMe.putFloat(vec.z())
                }

                GLSLBaseType.VEC4.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector4fc ?: zero4
                    fillMe.putFloat(vec.x())
                    fillMe.putFloat(vec.y())
                    fillMe.putFloat(vec.z())
                    fillMe.putFloat(vec.w())
                }

                GLSLBaseType.IVEC2.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector2ic ?: zero2i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                }

                GLSLBaseType.IVEC3.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector3ic ?: zero3i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                    fillMe.putInt(vec.z())
                }

                GLSLBaseType.IVEC4.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector4ic ?: zero4i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                    fillMe.putInt(vec.z())
                    fillMe.putInt(vec.w())
                }

                GLSLBaseType.UVEC2.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector2ic ?: zero2i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                }

                GLSLBaseType.UVEC3.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector3ic ?: zero3i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                    fillMe.putInt(vec.z())
                }

                GLSLBaseType.UVEC4.fieldType -> {
                    val vec = field.property.getter.call(interfaceBlock) as? Vector4ic ?: zero4i
                    fillMe.putInt(vec.x())
                    fillMe.putInt(vec.y())
                    fillMe.putInt(vec.z())
                    fillMe.putInt(vec.w())
                }

                GLSLBaseType.MAT4.fieldType -> {
                    val mat4 = field.property.getter.call(interfaceBlock) as? Matrix4fc ?: mat4identity
                    mat4.get(fillMe)
                    //fillMe.position(fillMe.position())
                }

                else -> throw Exception("field type ${field.type} does not have a byte buffer translation branch")
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

        val zero2i = Vector2i(0)
        val zero3i = Vector3i(0)
        val zero4i = Vector4i(0)

        val mat4identity = Matrix4f()
    }
}