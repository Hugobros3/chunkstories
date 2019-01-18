package xyz.chunkstories.graphics.common.shaders

import org.joml.*
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

sealed class GLSLType(val glslToken: String, val alignment: Int, val size: Int) {
    sealed class BaseType(glslToken: String, val classes: KClass<*>, alignment: Int, size: Int) : GLSLType(glslToken, alignment, size) {
        /** Integer map easily */
        object GlslInt : BaseType("int", Int::class, 4, 4)

        object GlslIVec2 : BaseType("ivec2", Vector2ic::class, 8, 8)
        object GlslIVec3 : BaseType("ivec3", Vector3ic::class, 16, 8)
        object GlslIVec4 : BaseType("ivec4", Vector4ic::class, 16, 8)

        /** The JVM doesn't really have unsigned integers, so we map GLSL uints to JVM ints */
        object GlslUInt : BaseType("uint", Int::class, 4, 4)

        object GlslUVec2 : BaseType("uvec2", Vector2ic::class, 8, 8)
        object GlslUVec3 : BaseType("uvec3", Vector3ic::class, 16, 8)
        object GlslUVec4 : BaseType("uvec4", Vector4ic::class, 16, 8)

        /** Java longs map to GLSL ints */
        object GlslLong : BaseType("int", Long::class, 4, 4)

        object GlslFloat : BaseType("float", Float::class, 4, 4)
        object GlslVec2 : BaseType("vec2", Vector2fc::class, 8, 8)
        object GlslVec3 : BaseType("vec3", Vector3fc::class, 16, 8)
        object GlslVec4 : BaseType("vec4", Vector4fc::class, 16, 8)

        object GlslMat3 : BaseType("mat3", Matrix3fc::class, 16, 36)
        object GlslMat4 : BaseType("mat4", Matrix4fc::class, 16, 64)

        companion object {
            val list: List<GLSLType.BaseType> = GLSLType.BaseType::class.nestedClasses.mapNotNull {
                it.objectInstance as? GLSLType.BaseType
            }

            //TODO find a way to ensure we'll never map Int::class to uint by accident
            fun get(kClass: KClass<*>): GLSLType? = list.find { kClass.isSubclassOf(it.classes) }
            fun get(glslToken: String) = list.find { it.glslToken == glslToken }
        }
    }

    class Array(val baseType: GLSLType, val elements: Int) :
            GLSLType("${baseType.glslToken}[${if (elements > 0) "$elements" else ""}]", 16, elements * 16)

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

object GLSLBaseTypes {
    val list: List<GLSLType.BaseType> = GLSLType.BaseType::class.nestedClasses.map {
        it.objectInstance as GLSLType.BaseType
    }

}

data class JvmStructField(val name: String, val offset: Int, val type: GLSLType, val property: KProperty<*>)

/*
enum class GLSLBaseType(val klass: KClass<*>, val alignment: Int, val size: Int, val glslToken: String) {
    FLOAT(Float::class, 4, 4, "float"),
    INT(Int::class, 4, 4, "int"),
    // Signed ? Unsigned ? The JVM doesn't know !
    UINT(Int::class, 4, 4, "uint"),

    // For now double-precision still gets mapped to single precision
    LONG(Long::class, 4, 4, "int"),

    VEC2(Vector2fc::class, 2 * 4, 2 * 4, "vec2"),
    VEC3(Vector3fc::class, 4 * 4, 3 * 4, "vec3"),
    VEC4(Vector4fc::class, 4 * 4, 4 * 4, "vec4"),

    // For now double-precision still gets mapped to single precision
    VEC2D(Vector2dc::class, 2 * 4, 2 * 4, "vec2"),
    VEC3D(Vector3dc::class, 4 * 4, 3 * 4, "vec3"),
    VEC4D(Vector4dc::class, 4 * 4, 4 * 4, "vec4"),

    IVEC2(Vector2ic::class, 2 * 4, 2 * 4, "ivec2"),
    IVEC3(Vector3ic::class, 4 * 4, 3 * 4, "ivec3"),
    IVEC4(Vector4ic::class, 4 * 4, 4 * 4, "ivec4"),

    UVEC2(Vector2ic::class, 2 * 4, 2 * 4, "uvec2"),
    UVEC3(Vector3ic::class, 4 * 4, 3 * 4, "uvec3"),
    UVEC4(Vector4ic::class, 4 * 4, 4 * 4, "uvec4"),

    MAT3(Matrix3fc::class, 4 * 4, 3 * 3 * 4, "mat3"),
    MAT4(Matrix4fc::class, 4 * 4, 4 * 4 * 4, "mat4"),
    ;

    /*val fieldType: InterfaceBlockFieldType

    init {
        fieldType = InterfaceBlockBaseFieldType(klass, glslToken, alignment, size)
    }*/

    companion object {
        fun get(kClass: KClass<*>): GLSLType? = values().find { kClass.isSubclassOf(it.klass) }

        fun get(name: String): GLSLType? = values().find { it.glslToken == name }
    }

    override fun toString(): String {
        return "GLSLBaseType(klass=$klass, glslToken='$glslToken', alignment=$alignment, size=$size, fieldType=fieldType)"
    }
}*/