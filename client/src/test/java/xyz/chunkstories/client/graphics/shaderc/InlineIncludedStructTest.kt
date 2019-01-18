package xyz.chunkstories.client.graphics.shaderc
/*
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.graphics.common.shaders.PreprocessedShaderStage
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
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

        val factory = ShaderCompiler(this.javaClass.classLoader)
        val meta = PreprocessedShaderStage(factory, shaderCode, ShaderStage.VERTEX)

        println("Processed GLSL :\n${meta.transformedCode}")
    }
}*/