package xyz.chunkstories.client

import io.xol.chunkstories.graphics.common.ShaderMetadata
import io.xol.chunkstories.graphics.vulkan.shaderc.InlineUniformStructs
import io.xol.chunkstories.graphics.vulkan.shaderc.InlineUniformStructs.inlineStructsUsedAsUniformTypes
import org.junit.Test

class InlineIncludedStructTest {

    @Test
    fun inlineIncludedStructTest() {
        val shaderCode = """
            #version 330

            #include struct <${TestStructure::class.qualifiedName}>
            layout(std140) uniform TestStructure test;

            out vec4 fragColor;

            void main() {
                if(test.inter == 1) {
                    fragColor = vec4(test.floater, test.floater, test.floater, 1.0);
                }
            }
        """.trimIndent()

        val meta = ShaderMetadata(shaderCode, this::class.java)

        val generatedGLSLCode = meta.glslWithAddedStructs
        val processedGLSLCode = inlineStructsUsedAsUniformTypes(generatedGLSLCode, meta)

        println("Processed GLSL :\n$processedGLSLCode")
    }
}