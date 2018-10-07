package io.xol.chunkstories.graphics.common.shaderc

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass

open class ShaderFactory(open val classLoader: ClassLoader) {

    enum class GLSLDialect {
        OPENGL4,
        VULKAN
    }

    /** All the data structure that are explicitely included in the shader string *or* implicitely included due to use in an included struct */
    val structures = mutableMapOf<KClass<InterfaceBlock>, InterfaceBlockGLSLMapping>()

    fun translateGLSL(dialect: GLSLDialect, stagesCode: Map<ShaderStage, String>): GLSLProgram =
            SpirvCrossHelper.translateGLSLDialect(this, dialect, stagesCode)

    /**
     * Vulkan GLSL semantics don't like loose non-opaque uniforms, and so we use interface blocks.
     *
     * We can `#include struct` in GLSL to generate a struct matching a Java class, but GLSLang actually wants the uniform declaration to
     * *declare* a interface block, even when all we want is to use the same layout as a struct. This is legal in OpenGL GLSL, but not
     * Vulkan GLSL. Since I find this utterly dumb, here is a preprocessor that explodes the struct definition at the uniform declaration
     */
    //TODO prolly should just parse the GLSL at this point and inline any struct we can ? idk
    fun PreprocessedProgram.findAndInlineUBOs(): List<Pair<String, InterfaceBlockGLSLMapping>> {
        val list = mutableListOf<Pair<String, InterfaceBlockGLSLMapping>>()
        var processed = ""

        for (line in this.transformedCode.lines()) {
            var layoutQualifier = ""
            var declarationStrippedOfLayoutPrefix = line

            var translated: String? = line

            if (line.startsWith("layout")) {
                val layoutEndIndex = line.indexOf(')') + 1
                layoutQualifier = line.substring(0, layoutEndIndex)
                declarationStrippedOfLayoutPrefix = line.substring(layoutEndIndex).trim()
                //println("layout: $layoutQualifier $declarationStrippedOfLayoutPrefix")

                // If the layout block is on a line of it's own it'll just get forgotten
                // translated = null
            }


            if (declarationStrippedOfLayoutPrefix.startsWith("uniform")) {
                val uniformTypeName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(1)
                val uniformName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(2)?.trimEnd(';')

                factory.structures.values.find { it.glslToken == uniformTypeName }?.apply {
                    //this.sampleInstance as? UniformBlock ?: throw Exception("You must declare your InterfaceBlock as UniformBlock to use it in a Shader !")

                    println("Found inlineable uniform using a InterfaceBlock-based struct: $uniformName")
                    list += Pair(uniformName!!, this)

                    val inlinedUniformInterfaceBlockDeclaration = "layout(std140) uniform _inlined${uniformTypeName}_$uniformName {\n" + this.generateInnerGLSL() + "} $uniformName;\n"
                    translated = inlinedUniformInterfaceBlockDeclaration
                }
            }

            if (translated != null)
                processed += translated + "\n"
        }

        this.transformedCode = processed

        return list
    }

    data class GLSLProgram(val sourceCode: Map<ShaderStage, String>, val resources: List<GLSLUniformResource>)

    interface GLSLUniformResource {
        val name: String
        val descriptorSet: Int
        val binding: Int
    }

    data class GLSLUniformBlock(
            override val name: String,
            override val descriptorSet: Int,
            override val binding: Int,
            val mapper: InterfaceBlockGLSLMapping) : GLSLUniformResource

    data class GLSLUnusedUniform(
            override val name: String,
            override val descriptorSet: Int,
            override val binding: Int
    ) : GLSLUniformResource

    /** Represents a (potentially an array of) sampler2D uniform resource declared in one of the stages of the GLSL program */
    data class GLSLUniformSampler2D(
            override val name: String,
            override val descriptorSet: Int,
            override val binding: Int,
            val count: Int) : GLSLUniformResource
}