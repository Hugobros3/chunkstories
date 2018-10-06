package io.xol.chunkstories.graphics.common.shaderc

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import kotlin.reflect.KClass

open class ShaderFactory(open val classLoader: ClassLoader) {

    /** All the data structure that are explicitely included in the shader string *or* implicitely included due to use in an included struct */
    val structures = mutableMapOf<KClass<InterfaceBlock>, InterfaceBlockGLSLMapping>()

    fun translateGLSL(dialect: GLSLDialect, stagesCode: Map<ShaderStage, String>): SpirvCrossHelper.TranspiledGLSLProgram? =
            SpirvCrossHelper.translateGLSLDialect(this, dialect, stagesCode)

    /**
     * Vulkan GLSL semantics don't like loose non-opaque uniforms, and so we use interface blocks.
     *
     * We can `#include struct` in GLSL to generate a struct matching a Java class, but GLSLang actually wants the uniform declaration to
     * *declare* a interface block, even when all we want is to use the same layout as a struct. This is legal in OpenGL GLSL, but not
     * Vulkan GLSL. Since I find this utterly dumb, here is a preprocessor that explodes the struct definition at the uniform declaration
     */
    //TODO prolly should just parse the GLSL at this point and inline any struct we can ? idk
    fun inlineStructsUsedAsUniformTypes(shaderMetadata: ShaderWithResolvedIncludeStructs): String {
        var processed = ""

        for (line in shaderMetadata.transformedCode.lines()) {
            var layoutQualifier = ""
            var declarationStrippedOfLayoutPrefix = line

            var translated : String? = line

            if (line.startsWith("layout")) {
                val layoutEndIndex = line.indexOf(')') + 1
                layoutQualifier = line.substring(0, layoutEndIndex)
                declarationStrippedOfLayoutPrefix = line.substring(layoutEndIndex).trim()
                println("layout: $layoutQualifier $declarationStrippedOfLayoutPrefix")

                // If the layout block is on a line of it's own it'll just get forgotten
                translated = null
            }

            if (declarationStrippedOfLayoutPrefix.startsWith("uniform")) {
                val uniformTypeName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(1) ?: "prout prout caca boudin"
                val uniformName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(2)?.trimEnd(';') ?: "prout prout caca boudin"

                this.structures.values.find { it.glslToken == uniformTypeName }?.apply {
                    println("Found inlineable uniform using a InterfaceBlock-based struct")

                    val inlinedUniformInterfaceBlockDeclaration =
                            "layout(std140) uniform ${this.glslToken}_inlined_for_$uniformName {\n" + this.generateInnerGLSL() + "} $uniformName;\n"

                    translated = inlinedUniformInterfaceBlockDeclaration
                }
            }

            if(translated != null)
                processed += translated + "\n"
        }

        return processed
    }

    enum class GLSLDialect {
        OPENGL4,
        VULKAN
    }
}