package io.xol.chunkstories.graphics.common.shaderc

import org.joml.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

enum class GLSLBaseType(val klass: KClass<*>, val glslToken: String, val alignment: Int, val size: Int) {
    FLOAT(Float::class, "float", 4, 4),
    INT(Int::class, "int", 4, 4),
    LONG(Long::class, "int", 4, 4),

    VEC2(Vector2fc::class, "vec2", 2 * 4, 2 * 4),
    VEC3(Vector3fc::class, "vec3", 4 * 4, 3 * 4),
    VEC4(Vector4fc::class, "vec4", 4 * 4, 4 * 4),

    MAT3(Matrix3fc::class, "mat3", 4 * 4, 3 * 3 * 4),
    MAT4(Matrix4fc::class, "mat4", 4 * 4, 4 * 4 * 4),
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