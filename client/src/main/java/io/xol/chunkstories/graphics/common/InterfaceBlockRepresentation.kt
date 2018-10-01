package io.xol.chunkstories.graphics.common

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.graphics.vulkan.shaderc.hex
import org.joml.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaField

class InterfaceBlockRepresentation(val klass: KClass<InterfaceBlock>, shaderMetadata: ShaderMetadata) : InterfaceBlockFieldType(klass) {
    private val interfaceBlockClass = klass.findOutActualInterfaceBlockClass()
    private val sampleInstance: InterfaceBlock

    override val glslToken: String = interfaceBlockClass.simpleName ?: throw Exception("Don't use anonymous classes for writing InterfaceBlocks !")

    val fields : Array<InterfaceBlockField>
    internal val requirements = mutableListOf<KClass<InterfaceBlock>>()

    init {
        if(shaderMetadata.stack.contains(klass))
            throw Exception("Loop detected! Interface block ${klass.qualifiedName} was referenced from itself !")

        shaderMetadata.stack.add(0, klass)

        val fields = mutableListOf<InterfaceBlockField>()
        var currentOffset = 0

        sampleInstance = klass.constructors.find { it.parameters.isEmpty() }?.call()
                ?: throw Exception("Any structure implementing InterfaceBlock MUST have a default constructor")
        for (property in klass.memberProperties) {
            // Check the property is a concrete one
            val declaredIn = property.javaField?.declaringClass ?: continue
            // Check it's declared in the class that explicitely implements InterfaceBlock
            if (!declaredIn.isAssignableFrom(interfaceBlockClass.java)) {
                println("Field ${property.name} is declared in child class $declaredIn, ignoring")
                continue
            }
            // We'll need that
            property.javaField!!.isAccessible = true

            fun translateFieldType(type: KClass<out Any>, value: Any?): InterfaceBlockFieldType {
                val staticType = StaticDataTypes.list.find { it.kClass == type }
                return when {
                    // Base GLSL types
                    staticType != null -> {
                        staticType
                    }

                    // Array types - Arrays *have* to be non-null
                    value is Array<*> -> {
                        if (value.size > 0) {
                            val element0 = value[0]
                            val element0Type = translateFieldType(value::class.java.componentType.kotlin, element0)
                            InterfaceBlockArrayTypeType(type as KClass<Array<*>>, value.size, element0Type)
                        } else throw Exception("Who uses zero sized arrays ???")
                    }
                    value is IntArray -> {
                        if (value.size > 0) {
                            InterfaceBlockArrayTypeType(type as KClass<Array<*>>, value.size, StaticDataTypes.get(Int::class))
                        } else throw Exception("Who uses zero sized arrays ???")
                    }
                    value is FloatArray -> {
                        if (value.size > 0) {
                            InterfaceBlockArrayTypeType(type as KClass<Array<*>>, value.size, StaticDataTypes.get(Float::class))
                        } else throw Exception("Who uses zero sized arrays ???")
                    }

                    // We're referencing another struct !
                    InterfaceBlock::class.java.isAssignableFrom(type.java) -> {
                        val type = type as KClass<InterfaceBlock>
                        var structRepresentation = shaderMetadata.structures.find { it.interfaceBlockClass == type }
                        if(structRepresentation == null) {
                            structRepresentation = InterfaceBlockRepresentation(type, shaderMetadata)

                            shaderMetadata.done += structRepresentation.klass
                            shaderMetadata.structures += structRepresentation
                        }

                        requirements.add(type)

                        structRepresentation
                    }

                    else -> throw Exception("No idea what to do with type $type in property ${property.name} of class ${interfaceBlockClass.qualifiedName}")
                }
            }

            val value = property.get(sampleInstance)
            val type = property.javaField!!.type.kotlin

            val structFieldType = translateFieldType(type, value)

            // Maintain proper alignment
            val misalignment = currentOffset % structFieldType.alignment
            if (misalignment != 0) {
                //println("Current offset $currentOffset is misaligned for base alignment ${structFieldType.alignment} of the data type of ${property.name} (${structFieldType.glslToken})")
                currentOffset = (currentOffset / structFieldType.alignment) * structFieldType.alignment
                currentOffset += structFieldType.alignment
                //println("corrected to $currentOffset")
            }

            val structField = InterfaceBlockField(property.name, currentOffset, structFieldType)

            currentOffset += structField.type.size

            fields.add(structField)
            //fields.put(structField.offset, structField)
        }

        shaderMetadata.stack.removeAt(0)

        this.fields = fields.toTypedArray()
    }

    fun generateGLSL() : String {
        var glsl = "struct $glslToken {\n"

        glsl += generateInnerGLSL()

        glsl += "};\n"

        return glsl
    }

    fun generateInnerGLSL() : String {
        var glsl = ""
        for(field in fields) {
            glsl += "\t${field.type.glslToken} ${field.name};\n"
        }
        return glsl
    }

    override fun toString(): String {
        return """
            InterfaceBlockRepresentation($glslToken) {
                ${fields.map {
            "0x${it.offset.hex()} (${it.offset}): ${it.type.glslToken} ${it.name}"
        }}
            }
            """.trimIndent()
    }
}

private fun KClass<out InterfaceBlock>.findOutActualInterfaceBlockClass(): KClass<out InterfaceBlock> {
    var cl = this

    while (cl != cl.superclass()) {
        if (cl.superclasses.contains(InterfaceBlock::class))
            break
        cl = cl.superclass() as KClass<out InterfaceBlock>
    }

    return cl
}

private fun KClass<*>.superclass() = this.superclasses.filter { !it.java.isInterface }.singleOrNull() ?: this

data class InterfaceBlockField(val name: String, val offset: Int, val type: InterfaceBlockFieldType)

open abstract class InterfaceBlockFieldType(val kClass: KClass<*>) {
    abstract val glslToken: String

    /** Alignment (used for std140 layout */
    open val alignment = 4
    open val size = 4
}

private class InterfaceBlockArrayTypeType(kClass: KClass<Array<*>>, val arraySize: Int, val elementsType: InterfaceBlockFieldType) : InterfaceBlockFieldType(kClass) {
    override val glslToken: String
    override val alignment = 4 * 4
    override val size = elementsType.size * arraySize

    init {
        val childToken = elementsType.glslToken
        // We need the brackets in the right order
        if (childToken.contains('[')) {
            val index = childToken.indexOf('[')
            val scalarType = childToken.subSequence(0, index)
            val rest = childToken.substring(index)
            glslToken = "$scalarType[$arraySize]$rest"
        } else
            glslToken = "$childToken[$arraySize]"
    }
}

private class InterfaceBlockStaticFieldType(kClass: KClass<*>, override val glslToken: String, override val alignment: Int, override val size: Int) : InterfaceBlockFieldType(kClass)

object StaticDataTypes {
    fun get(kClass: KClass<*>): InterfaceBlockFieldType = list.find { it.kClass == kClass } ?: throw Exception("Unknown static type $kClass")

    val list: List<InterfaceBlockFieldType> = listOf(
            InterfaceBlockStaticFieldType(Float::class, "float", 4, 4),
            InterfaceBlockStaticFieldType(Int::class, "int", 4, 4),
            InterfaceBlockStaticFieldType(Long::class, "int", 4, 4),

            InterfaceBlockStaticFieldType(Vector2f::class, "vec2", 2 * 4, 2 * 4),
            InterfaceBlockStaticFieldType(Vector3f::class, "vec3", 4 * 4, 3 * 4),
            InterfaceBlockStaticFieldType(Vector4f::class, "vec4", 4 * 4, 4 * 4),

            InterfaceBlockStaticFieldType(Matrix3f::class, "mat3", 4 * 4, 3 * 3 * 4),
            InterfaceBlockStaticFieldType(Matrix4f::class, "mat4", 4 * 4, 4 * 4 * 4)
    )
}
