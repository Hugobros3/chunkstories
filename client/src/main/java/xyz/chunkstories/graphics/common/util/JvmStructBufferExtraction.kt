package xyz.chunkstories.graphics.common.util

import org.joml.*
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.JvmStructField
import java.nio.ByteBuffer

fun getStd140AlignedSizeForStruct(struct: GLSLType.JvmStruct): Int {
    //val instancedStruct = glslProgram.instancedInputs.find { it.name == name } ?: throw Exception("No instanced input named: $name")
    val structSize = struct.size
    val sizeAligned16 = if (structSize % 16 == 0) structSize else (structSize / 16 * 16) + 16
    return sizeAligned16
}

fun extractInterfaceBlock(target: ByteBuffer, offsetInTarget: Int = 0, instance: InterfaceBlock, struct: GLSLType.JvmStruct) {
    for (field in struct.fields) {
        target.position(offsetInTarget + field.offset)
        extractInterfaceBlockField(target, instance, field)
    }
}

private fun extractInterfaceBlockField(target: ByteBuffer, instance: InterfaceBlock, field: JvmStructField) {
    val data = field.property.getter.call(instance)

    when (field.type) {
        is GLSLType.BaseType -> extractBaseTypeRawData(field.type, data, target)
        is GLSLType.Array -> {
            val basePosition = target.position()
            when {
                data is IntArray -> {
                    for ((i, element) in data.withIndex()) {
                        val baseType = field.type.baseType

                        target.position(basePosition + i * baseType.inArraySize)

                        /*val baseTypeAlignment = baseType.alignment
                        if(target.position() % baseTypeAlignment != 0) {
                            target.position((target.position() / baseTypeAlignment) * baseTypeAlignment + baseTypeAlignment)
                        }*/

                        when(baseType) {
                            is GLSLType.BaseType -> {
                                extractBaseTypeRawData(baseType, element, target)
                            }
                            else -> TODO()
                        }
                    }

                    return
                }
            }

            val array = data as? Array<*> ?: throw Exception("Not an array !")
            for ((i, element) in array.withIndex()) {
                val baseType = field.type.baseType

                target.position(basePosition + i * baseType.inArraySize)

                /*val baseTypeAlignment = baseType.alignment
                if(target.position() % baseTypeAlignment != 0) {
                    target.position((target.position() / baseTypeAlignment) * baseTypeAlignment + baseTypeAlignment)
                }*/

                when(baseType) {
                    is GLSLType.BaseType -> {
                        extractBaseTypeRawData(baseType, element, target)

                    }
                    is GLSLType.JvmStruct -> {
                        val basePos = target.position()
                        val mapper = baseType
                        val dataAsIb = element as InterfaceBlock

                        for (field in mapper.fields) {
                            target.position(field.offset + basePos)
                            //println("${field.name} ${fillMe.position()}")
                            extractInterfaceBlockField(target, dataAsIb, field)
                        }
                    }
                    else -> TODO()
                }


                //println("element:${baseType.size} ${baseType.alignment}"+fillMe)
            }
        }
        is GLSLType.JvmStruct -> {
            val basePos = target.position()
            val mapper = field.type
            val dataAsIb = data as InterfaceBlock

            for (field in mapper.fields) {
                target.position(field.offset + basePos)
                //println("${field.name} ${fillMe.position()}")
                extractInterfaceBlockField(target, dataAsIb, field)
            }
        }
        //else -> throw Exception("field type ${field.type} does not have a byte buffer translation branch")
    }
}

private fun extractBaseTypeRawData(baseType: GLSLType.BaseType, data: Any?, fillMe: ByteBuffer) {
    when (baseType) {
        GLSLType.BaseType.GlslFloat -> fillMe.putFloat(data as Float)
        GLSLType.BaseType.GlslInt -> fillMe.putInt(data as Int)
        GLSLType.BaseType.GlslUInt -> fillMe.putInt(data as Int)
        GLSLType.BaseType.GlslLong -> fillMe.putInt((data as Long).toInt())

        // SP vectors
        GLSLType.BaseType.GlslVec2 -> {
            val vec = data as? Vector2fc ?: GLSLDefaults.zero2
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
        }

        GLSLType.BaseType.GlslVec3 -> {
            val vec = data as? Vector3fc ?: GLSLDefaults.zero3
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
            fillMe.putFloat(vec.z())
        }

        GLSLType.BaseType.GlslVec4 -> {
            val vec = data as? Vector4fc ?: GLSLDefaults.zero4
            fillMe.putFloat(vec.x())
            fillMe.putFloat(vec.y())
            fillMe.putFloat(vec.z())
            fillMe.putFloat(vec.w())
        }

        // DP vectors
        GLSLType.BaseType.GlslVec2d -> {
            val vec = data as? Vector2dc ?: GLSLDefaults.zero2d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
        }

        GLSLType.BaseType.GlslVec3d -> {
            val vec = data as? Vector3dc ?: GLSLDefaults.zero3d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
            fillMe.putFloat(vec.z().toFloat())
        }

        GLSLType.BaseType.GlslVec4d -> {
            val vec = data as? Vector4dc ?: GLSLDefaults.zero4d
            fillMe.putFloat(vec.x().toFloat())
            fillMe.putFloat(vec.y().toFloat())
            fillMe.putFloat(vec.z().toFloat())
            fillMe.putFloat(vec.w().toFloat())
        }

        // Int vectors
        GLSLType.BaseType.GlslIVec2 -> {
            val vec = data as? Vector2ic ?: GLSLDefaults.zero2i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
        }

        GLSLType.BaseType.GlslIVec3 -> {
            val vec = data as? Vector3ic ?: GLSLDefaults.zero3i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
        }

        GLSLType.BaseType.GlslIVec4 -> {
            val vec = data as? Vector4ic ?: GLSLDefaults.zero4i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
            fillMe.putInt(vec.w())
        }

        // UInt vectors
        GLSLType.BaseType.GlslUVec2 -> {
            val vec = data as? Vector2ic ?: GLSLDefaults.zero2i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
        }

        GLSLType.BaseType.GlslUVec3 -> {
            val vec = data as? Vector3ic ?: GLSLDefaults.zero3i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
        }

        GLSLType.BaseType.GlslUVec4 -> {
            val vec = data as? Vector4ic ?: GLSLDefaults.zero4i
            fillMe.putInt(vec.x())
            fillMe.putInt(vec.y())
            fillMe.putInt(vec.z())
            fillMe.putInt(vec.w())
        }

        GLSLType.BaseType.GlslMat4 -> {
            val mat4 = data as? Matrix4fc ?: GLSLDefaults.mat4identity
            mat4.get(fillMe)
            //fillMe.position(fillMe.position())
        }

        GLSLType.BaseType.GlslMat3 -> {
            val mat3 = data as? Matrix3fc ?: GLSLDefaults.mat3identity
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

        GLSLType.BaseType.GlslDouble -> TODO()
    }
}

object GLSLDefaults {
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