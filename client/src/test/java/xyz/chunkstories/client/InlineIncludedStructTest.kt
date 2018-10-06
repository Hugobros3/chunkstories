package xyz.chunkstories.client

import io.xol.chunkstories.graphics.common.shaderc.ShaderWithResolvedIncludeStructs
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
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

        val factory = ShaderFactory(this.javaClass.classLoader)
        val meta = ShaderWithResolvedIncludeStructs(factory, shaderCode)

        val processedGLSLCode = factory.inlineStructsUsedAsUniformTypes(meta)

        println("Processed GLSL :\n$processedGLSLCode")
    }
}