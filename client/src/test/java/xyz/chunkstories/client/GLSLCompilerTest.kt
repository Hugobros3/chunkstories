package xyz.chunkstories.client

import graphics.scenery.spirvcrossj.Loader
import io.xol.chunkstories.graphics.vulkan.shaderc.SpirvCrossHelper
import io.xol.chunkstories.util.LogbackSetupHelper
import org.junit.Test

class GLSLCompilerTest {
    @Test
    fun testGLSLCompilationWithSpirvCrossJ() {
        LogbackSetupHelper.setupLoggingForTesting()

        val vertexShader = javaClass.getResource("/shaders/base.vert").readText()
        println("Loaded vertex shader OK")

        val fragmentShader = javaClass.getResource("/shaders/base.frag").readText()
        println("Loaded fragment shader OK")

        SpirvCrossHelper.generateSpirV(vertexShader, null,  fragmentShader)
    }


    @Test
    fun test330GLSL() {
        LogbackSetupHelper.setupLoggingForTesting()

        val vertexShader = javaClass.getResource("/shaders/blit.vert").readText()
        println("Loaded vertex shader OK")

        val fragmentShader = javaClass.getResource("/shaders/blit.frag").readText()
        println("Loaded fragment shader OK")

        SpirvCrossHelper.generateSpirV(vertexShader, null,  fragmentShader)
    }
}