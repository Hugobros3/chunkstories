package xyz.chunkstories.client.graphics.shaderc

import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.api.graphics.structs.InterfaceBlock
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.util.LogbackSetupHelper
import org.joml.Matrix4f
import org.joml.Vector4f
import org.junit.Test
/*
class GLSLCompilerTest {
    @Test
    fun testGLSLCompilationWithSpirvCrossJ() {
        LogbackSetupHelper.setupLoggingForTesting()
        val factory = ShaderCompiler(this.javaClass.classLoader)

        val vertexShader = javaClass.getResource("/shaders/simpleCase/simpleCase.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/simpleCase/simpleCase.frag").readText()
        println("Loaded fragment tShader OK")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        println("Transpiling for Vulkan...")
        val program = factory.translateGLSL(ShaderCompiler.GLSLDialect.VULKAN, stages)
        println(program)
    }

    @Test
    fun test330GLSL() {
        LogbackSetupHelper.setupLoggingForTesting()
        val factory = ShaderCompiler(this.javaClass.classLoader)

        val vertexShader = javaClass.getResource("/shaders/blit/blit.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/blit/blit.frag").readText()
        println("Loaded fragment tShader OK")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        println("Transpiling for Vulkan...")
        val program = factory.translateGLSL(ShaderCompiler.GLSLDialect.VULKAN, stages)
        println(program)
    }

    @Test
    fun testWithIncludeStruct() {
        LogbackSetupHelper.setupLoggingForTesting()
        val factory = ShaderCompiler(this.javaClass.classLoader)

        val vertexShader = javaClass.getResource("/shaders/test.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/test.frag").readText()
        println("Loaded fragment tShader OK")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        println("Transpiling for Vulkan...")
        val vulkanProgram = factory.translateGLSL(ShaderCompiler.GLSLDialect.VULKAN, stages)!!
        println("Result: \n${vulkanProgram}")
    }

    @Test
    fun testCompareVkOpenglSemantics() {
        LogbackSetupHelper.setupLoggingForTesting()
        val factory = ShaderCompiler(this.javaClass.classLoader)

        val vertexShader = javaClass.getResource("/shaders/test.vert").readText()
        println("Loaded vertex tShader OK")

        val fragmentShader = javaClass.getResource("/shaders/test.frag").readText()
        println("Loaded fragment tShader OK")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        println("Transpiling for Vulkan...")
        val vulkanProgram = factory.translateGLSL(ShaderCompiler.GLSLDialect.VULKAN, stages)!!
        println("Result fs : \n${vulkanProgram.sourceCode[ShaderStage.FRAGMENT]}")

        println("Transpiling for OpenGL...")
        val openglProgram = factory.translateGLSL(ShaderCompiler.GLSLDialect.OPENGL4, stages)!!
        println("Result fs : \n${openglProgram.sourceCode[ShaderStage.FRAGMENT]}")
    }
}*/

class CameraTest : InterfaceBlock {
    var viewMatrix = Matrix4f()
}

class FullscreenFillColor : InterfaceBlock {
    var color = Vector4f(1.0F)
}