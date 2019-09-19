package xyz.chunkstories.graphics.common.shaders

import org.joml.*
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.hex
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

sealed class GLSLType(val glslToken: String, val alignment: Int, val size: Int) {
    val inArraySize = align(size, 16)

    sealed class BaseType(glslToken: String, val classes: KClass<*>, alignment: Int, size: Int, val dontMapFromGLSL: Boolean) : GLSLType(glslToken, alignment, size) {
        /** Integer map easily */
        object GlslInt : BaseType("int", Int::class, 4, 4, false)

        object GlslIVec2 : BaseType("ivec2", Vector2ic::class, 8, 8, false)
        object GlslIVec3 : BaseType("ivec3", Vector3ic::class, 16, 12, false)
        object GlslIVec4 : BaseType("ivec4", Vector4ic::class, 16, 16, false)

        /** The JVM doesn't really have unsigned integers, so we map GLSL uints to JVM ints */
        object GlslUInt : BaseType("uint", Int::class, 4, 4, false)

        object GlslUVec2 : BaseType("uvec2", Vector2ic::class, 8, 8, false)
        object GlslUVec3 : BaseType("uvec3", Vector3ic::class, 16, 12, false)
        object GlslUVec4 : BaseType("uvec4", Vector4ic::class, 16, 16, false)

        /** Java longs map to GLSL ints */
        object GlslLong : BaseType("int", Long::class, 8, 4, true)

        object GlslFloat : BaseType("float", Float::class, 4, 4, false)
        object GlslVec2 : BaseType("vec2", Vector2fc::class, 8, 8, false)
        object GlslVec3 : BaseType("vec3", Vector3fc::class, 16, 12, false)
        object GlslVec4 : BaseType("vec4", Vector4fc::class, 16, 16, false)

        object GlslDouble: BaseType("float", Double::class, 4, 4, true)
        object GlslVec2d : BaseType("vec2", Vector2dc::class, 8, 8, true)
        object GlslVec3d : BaseType("vec3", Vector3dc::class, 16, 12, true)
        object GlslVec4d : BaseType("vec4", Vector4dc::class, 16, 16, true)

        object GlslMat3 : BaseType("mat3", Matrix3fc::class, 16, 36, false)
        object GlslMat4 : BaseType("mat4", Matrix4fc::class, 16, 64, false)

        companion object {
            val list: List<GLSLType.BaseType> = GLSLType.BaseType::class.nestedClasses.mapNotNull {
                it.objectInstance as? GLSLType.BaseType
            }

            //TODO find a way to ensure we'll never map Int::class to uint by accident
            fun get(kClass: KClass<*>): GLSLType? = list.find { kClass.isSubclassOf(it.classes) }
            fun get(glslToken: String) = list.find { !it.dontMapFromGLSL && it.glslToken == glslToken }
        }
    }

    class Array(val baseType: GLSLType, val elements: Int) :
            GLSLType("${baseType.glslToken}[${if (elements > 0) "$elements" else ""}]", 16, elements * baseType.inArraySize)

    class JvmStruct(glslToken: String, val kClass: KClass<InterfaceBlock>, val fields: List<JvmStructField>, alignment: Int, size: Int) : GLSLType(glslToken, alignment, size) {
        override fun toString(): String {
            return """
            JvmStruct($glslToken) {
                ${fields.map {
                "0x${it.offset.hex()} (${it.offset}): ${it.type.glslToken} ${it.name}"
            }}
            }
            """.trimIndent()
        }
    }
}

data class JvmStructField(val name: String, val offset: Int, val type: GLSLType, val property: KProperty<*>)

fun align(size: Int, alignment: Int): Int {
    val d = size / alignment
    if(d * alignment == size)
        return size
    else
        return (d+1) * alignment
}