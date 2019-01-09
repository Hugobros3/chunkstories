package xyz.chunkstories.graphics.common.shaderc

import org.joml.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

    val fieldType: InterfaceBlockFieldType

    init {
        fieldType = InterfaceBlockBaseFieldType(klass, glslToken, alignment, size)
    }

    companion object {
        fun get(kClass: KClass<*>): GLSLBaseType? = values().find { kClass.isSubclassOf(it.klass) }

        fun get(name: String): GLSLBaseType? = values().find { it.glslToken == name }
    }

    override fun toString(): String {
        return "GLSLBaseType(klass=$klass, glslToken='$glslToken', alignment=$alignment, size=$size, fieldType=$fieldType)"
    }
}