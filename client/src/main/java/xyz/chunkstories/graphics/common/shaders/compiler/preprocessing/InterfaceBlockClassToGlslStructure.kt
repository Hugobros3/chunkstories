package xyz.chunkstories.graphics.common.shaders.compiler.preprocessing

import xyz.chunkstories.api.graphics.structs.IgnoreGLSL
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.common.shaders.JvmStructField
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

/* This file deals with performing reflection on JVM types to extract a GLSLType.JvmStructure that can be used
to provide data to shaders.
 */

/** Lookups some JVM class and will launch the reflection process if it hasn't run already */
fun ShaderCompiler.getGlslStruct(kclass: KClass<InterfaceBlock>): GLSLType.JvmStruct =
        jvmGlslMappings[kclass] ?: createGLSLStructFromJVMClass(kclass, emptySet())

/**
 * Builds a GLSLType.JvmStructure from a JVM class. May generate additional JvmStructure.
 * Will throw an exception of one InterfaceBlock class has a member actualProperty that could allow for loops in the data structure.
 */
fun ShaderCompiler.createGLSLStructFromJVMClass(klass: KClass<InterfaceBlock>, banned: Set<KClass<InterfaceBlock>>): GLSLType.JvmStruct {
    val structName = klass.simpleName ?: throw Exception("Don't use anonymous classes for use as GLSL structs !")

    // Sometimes shaders may reference a class that doesn't implement the InterfaceBlock interface itself.
    // Not sure if it should even be allowed, but anyway we want to ignore the extra stuff the child class brings on
    // TODO re-evaluate the above problem and take a stance
    val legitClass = klass.findOutActualInterfaceBlockClass()

    val fields = mutableListOf<JvmStructField>()
    var currentOffset = 0

    // Create a dummy instance by either calling the default constructor, if available,
    // alternatively with all optional parameters left to their defaults.
    val sampleInstance = try {klass.constructors.find { it.parameters.isEmpty() }?.call() ?: let {
        klass.constructors.find { it.parameters.filter { !it.isOptional }.isEmpty() }?.callBy(emptyMap())
                ?: throw Exception("Any structure implementing InterfaceBlock MUST have a default constructor")
    } } catch(e: Exception) { throw Exception("Failed to instantiate sample InterfaceBlock", e) }

    for (property in legitClass.memberProperties) {
        if (property.annotations.find { it is IgnoreGLSL } != null) {
            ShaderCompiler.logger.debug("Field ${property.name} is declared with the @IgnoreGLSL annotation, ignoring")
            continue
        }

        // We'll need that
        property.javaField?.isAccessible = true

        fun jvmTypeToGlslType(propertyType: KClass<out Any>, value: Any?): GLSLType = GLSLType.BaseType.get(propertyType) ?: when {
            // Array types - Arrays *have* to be non-null
            value is Array<*> -> {
                if (value.size > 0) {
                    val element0 = value[0]
                    val element0Type = jvmTypeToGlslType(value::class.java.componentType.kotlin, element0)
                    GLSLType.Array(element0Type, value.size)
                } else throw Exception("Who uses zero sized arrays ???")
            }
            value is IntArray -> {
                if (value.size > 0) {
                    GLSLType.Array(GLSLType.BaseType.GlslInt, value.size)
                } else throw Exception("Who uses zero sized arrays ???")
            }
            value is FloatArray -> {
                if (value.size > 0) {
                    GLSLType.Array(GLSLType.BaseType.GlslFloat, value.size)
                } else throw Exception("Who uses zero sized arrays ???")
            }

            // We're referencing another struct !
            InterfaceBlock::class.java.isAssignableFrom(propertyType.java) -> {
                val propertyClass = propertyType as KClass<InterfaceBlock>

                jvmGlslMappings[propertyClass] ?: createGLSLStructFromJVMClass(propertyClass, banned.union(setOf(klass)))
            }

            else -> throw Exception("No idea what to do with type $propertyType in actualProperty ${property.name} of class ${legitClass.qualifiedName}")
        }

        val propertyValue = property.get(sampleInstance)
        val propertyType = property.returnType.jvmErasure//property.javaField?.type.kotlin

        val propertyGlslType = jvmTypeToGlslType(propertyType, propertyValue)

        // Maintain proper alignment
        if (currentOffset % propertyGlslType.alignment != 0) {
            currentOffset = (currentOffset / propertyGlslType.alignment) * propertyGlslType.alignment
            currentOffset += propertyGlslType.alignment
        }

        val structField = JvmStructField(property.name, currentOffset, propertyGlslType, property)
        fields += structField

        currentOffset += structField.type.size
    }

    val structSize = currentOffset
    val structAlignment =
            if (structSize % 16 != 0)
                (currentOffset / 16) * 16 + 16
            else currentOffset

    val jvmStruct = GLSLType.JvmStruct(structName, klass, fields, structAlignment, structSize)
    jvmGlslMappings[klass] = jvmStruct
    return jvmStruct
}

/* util methods */

/** */
private fun KClass<out InterfaceBlock>.findOutActualInterfaceBlockClass(): KClass<InterfaceBlock> {
    var cl = this

    while (cl != cl.superclass()) {
        if (cl.superclasses.contains(InterfaceBlock::class))
            break
        cl = cl.superclass() as KClass<out InterfaceBlock>
    }

    return cl as KClass<InterfaceBlock>
}

private fun KClass<*>.superclass() = this.superclasses.filter { !it.java.isInterface }.singleOrNull() ?: this
