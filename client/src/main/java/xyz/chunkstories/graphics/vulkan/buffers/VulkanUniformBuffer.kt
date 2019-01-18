package xyz.chunkstories.graphics.vulkan.buffers

import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.joml.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
import xyz.chunkstories.graphics.common.shaders.GLSLType

class VulkanUniformBuffer(backend: VulkanGraphicsBackend, val mapper: GLSLType.JvmStruct) :
        VulkanBuffer(backend, mapper.size.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, true) {

    fun upload(interfaceBlock: InterfaceBlock) {
        stackPush()

        /*val fillMe = stackMalloc(mapper.size)

        for (field in mapper.fields) {
            fillMe.position(field.offset)
            extractInterfaceBlockField(field, fillMe, interfaceBlock)
        }

        fillMe.position(0)
        fillMe.limit(fillMe.capacity())
        upload(fillMe)*/

        TODO()

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
/*
fun extractInterfaceBlockField(field: InterfaceBlockField, fillMe: ByteBuffer, interfaceBlock: InterfaceBlock) {
    when (field.type) {
        GLSLType.FLOAT.fieldType -> fillMe.putFloat(field.property.getter.call(interfaceBlock) as Float)
        GLSLType.INT.fieldType -> fillMe.putInt(field.property.getter.call(interfaceBlock) as Int)
        GLSLType.UINT.fieldType -> fillMe.putInt(field.property.getter.call(interfaceBlock) as Int)
        GLSLType.LONG.fieldType -> fillMe.putInt((field.property.getter.call(interfaceBlock) as Long).toInt())

        // SP vectors
        GLSLType.VEC2.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector2fc ?: VulkanUniformBuffer.zero2
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
        }

        GLSLType.VEC3.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector3fc ?: VulkanUniformBuffer.zero3
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
            fillMe.putFloat(vec.z())
        }

        GLSLType.VEC4.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector4fc ?: VulkanUniformBuffer.zero4
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
            fillMe.putFloat(vec.z())
            fillMe.putFloat(vec.w())
        }

        // DP vectors
        GLSLType.VEC2D.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector2dc ?: VulkanUniformBuffer.zero2d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
        }

        GLSLType.VEC3D.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector3dc ?: VulkanUniformBuffer.zero3d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
            fillMe.putFloat(vec.z().toFloat())
        }

        GLSLType.VEC4D.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector4dc ?: VulkanUniformBuffer.zero4d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
            fillMe.putFloat(vec.z().toFloat())
            fillMe.putFloat(vec.w().toFloat())
        }

        // Int vectors
        GLSLType.IVEC2.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector2ic ?: VulkanUniformBuffer.zero2i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
        }

        GLSLType.IVEC3.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector3ic ?: VulkanUniformBuffer.zero3i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
        }

        GLSLType.IVEC4.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector4ic ?: VulkanUniformBuffer.zero4i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
            fillMe.putInt(vec.w())
        }

        // UInt vectors
        GLSLType.UVEC2.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector2ic ?: VulkanUniformBuffer.zero2i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
        }

        GLSLType.UVEC3.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector3ic ?: VulkanUniformBuffer.zero3i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
        }

        GLSLType.UVEC4.fieldType -> {
            val vec = field.property.getter.call(interfaceBlock) as? Vector4ic ?: VulkanUniformBuffer.zero4i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
            fillMe.putInt(vec.w())
        }

        GLSLType.MAT4.fieldType -> {
            val mat4 = field.property.getter.call(interfaceBlock) as? Matrix4fc ?: VulkanUniformBuffer.mat4identity
            mat4.get(fillMe)
            //fillMe.position(fillMe.position())
        }

        GLSLType.MAT3.fieldType -> {
            val mat3 = field.property.getter.call(interfaceBlock) as? Matrix3fc ?: VulkanUniformBuffer.mat3identity
            val vec3 = Vector3f()
            for (i in 0..2) {
                mat3.getColumn(i, vec3)
                fillMe.putFloat(vec3.x)
                fillMe.putFloat(vec3.y)
                fillMe.putFloat(vec3.z)
                fillMe.putFloat(0f)
            }
            //fillMe.position(fillMe.position())
        }

        else -> throw Exception("field type ${field.type} does not have a byte buffer translation branch")
    }
}*/