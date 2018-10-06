package xyz.chunkstories.client

import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper
import io.xol.chunkstories.util.LogbackSetupHelper
import org.junit.Test

class GLSLCompilerTest {
    @Test
    fun testGLSLCompilationWithSpirvCrossJ() {
        LogbackSetupHelper.setupLoggingForTesting()

        val vertexShader = javaClass.getResource("/shaders/base.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/base.frag").readText()
        println("Loaded fragment tShader OK")

        val factory = ShaderFactory(this.javaClass.classLoader)
        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)
        factory.translateGLSL(ShaderFactory.GLSLDialect.VULKAN, stages)
    }


    @Test
    fun test330GLSL() {
        LogbackSetupHelper.setupLoggingForTesting()

        val vertexShader = javaClass.getResource("/shaders/blit.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/blit.frag").readText()
        println("Loaded fragment tShader OK")

        val factory = ShaderFactory(this.javaClass.classLoader)
        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)
        factory.translateGLSL(ShaderFactory.GLSLDialect.VULKAN, stages)
    }

    @Test
    fun testAdvanced() {
        LogbackSetupHelper.setupLoggingForTesting()

        val vertexShader = javaClass.getResource("/shaders/test.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/test.frag").readText()
        println("Loaded fragment tShader OK")

        val factory = ShaderFactory(this.javaClass.classLoader)
        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        println("Compiling for Vulkan...")
        val vulkanCode = factory.translateGLSL(ShaderFactory.GLSLDialect.VULKAN, stages)!!
        println("Result fs : \n${vulkanCode.stages[ShaderStage.FRAGMENT]}")

        println("Compiling for OpenGL...")
        val gl4Code = factory.translateGLSL(ShaderFactory.GLSLDialect.OPENGL4, stages)!!
        println("Result fs : \n${gl4Code.stages[ShaderStage.FRAGMENT]}")

    }
}