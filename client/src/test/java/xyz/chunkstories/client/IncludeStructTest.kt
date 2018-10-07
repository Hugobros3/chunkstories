package xyz.chunkstories.client

import io.xol.chunkstories.api.graphics.structs.InterfaceBlock
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.PreprocessedProgram
import org.joml.Matrix3f
import org.junit.Test

class TestSubStructure : InterfaceBlock {
    val a : Int = 8
    val b = -1
}

class TestStructure : InterfaceBlock {
    var floater : Float = 5.0F
    var inter : Int = 1
    var matrix : Matrix3f = Matrix3f()
    var values = FloatArray(5)
    val inc = arrayOf(TestSubStructure(), TestSubStructure())
    val nik = 9999999.0F
}

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

        val factory = ShaderFactory(this.javaClass.classLoader)

        val meta = PreprocessedProgram(factory, shaderCode)

        val generatedGLSLCode = meta.transformedCode
        println("Generated GLSL :\n$generatedGLSLCode")
    }
}