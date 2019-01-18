package xyz.chunkstories.client.graphics.shaderc
/*
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.PreprocessedShaderStage
import org.junit.Test

class IncludeStructTest {

    @Test
    fun testIncludeStruct() {
        val shaderCode = """
            #version 330

            #include struct <${TestStructure::class.qualifiedName}>

            out vec4 fragColor;

            main() {
                if(inter == 1) {
                    fragColor = vec4(floater, floater, floater)
                }
            }
        """.trimIndent()

        val factory = ShaderCompiler(this.javaClass.classLoader)

        val meta = PreprocessedShaderStage(factory, shaderCode, ShaderStage.VERTEX)

        val generatedGLSLCode = meta.transformedCode
        println("Generated GLSL :\n$generatedGLSLCode")
    }
}*/