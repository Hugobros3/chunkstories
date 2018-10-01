package io.xol.chunkstories.graphics.vulkan.shaderc

import io.xol.chunkstories.graphics.common.ShaderMetadata

/**
 * Vulkan GLSL semantics don't like loose non-opaque uniforms, and so we use interface blocks.
 *
 * We can `#include struct` in GLSL to generate a struct matching a Java class, but GLSLang actually wants the uniform declaration to
 * *declare* a interface block, even when all we want is to use the same layout as a struct. This is legal in OpenGL GLSL, but not
 * Vulkan GLSL. Since I find this utterly dumb, here is a preprocessor that explodes the struct definition at the uniform declaration
 */
object InlineUniformStructs {

    //TODO prolly should just parse the GLSL at this point and inline any struct we can ? idk
    fun inlineStructsUsedAsUniformTypes(shaderCode: String, shaderMetadata: ShaderMetadata): String {
        var processed = ""

        for (line in shaderCode.lines()) {
            var layoutQualifier = ""
            var declarationStrippedOfLayoutPrefix = line

            var translated = line

            if (line.startsWith("layout")) {
                val layoutEndIndex = line.indexOf(')') + 1
                layoutQualifier = line.substring(0, layoutEndIndex)
                declarationStrippedOfLayoutPrefix = line.substring(layoutEndIndex).trim()
                println("layout: $layoutQualifier $declarationStrippedOfLayoutPrefix")
            }

            if (declarationStrippedOfLayoutPrefix.startsWith("uniform")) {
                val uniformTypeName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(1) ?: "prout prout caca boudin"
                val uniformName = declarationStrippedOfLayoutPrefix.split(' ').getOrNull(2)?.trimEnd(';') ?: "prout prout caca boudin"

                shaderMetadata.structures.find { it.glslToken == uniformTypeName }?.apply {
                    println("Found inlineable uniform using a InterfaceBlock-based struct")

                    val inlinedUniformInterfaceBlockDeclaration =
                            "layout(std140) uniform ${this.glslToken}_inlined_for_$uniformName {\n" + this.generateInnerGLSL() + "} $uniformName;\n"

                    translated = inlinedUniformInterfaceBlockDeclaration
                }
            }

            processed += translated + "\n"
        }

        return processed
    }
}