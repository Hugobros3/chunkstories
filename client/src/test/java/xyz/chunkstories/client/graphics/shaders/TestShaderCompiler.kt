package xyz.chunkstories.client.graphics.shaders

import org.junit.Test
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.compiler.HeadlessShaderCompiler
import xyz.chunkstories.util.LogbackSetupHelper

class TestShaderCompiler {
    @Test
    fun simplestCase() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("simpleCase")
        println("OK")
        println(program.resources)
        println(program.sourceCode)
    }

    @Test
    fun usingStruct() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("usingStruct")
        println("OK")
        println(program.resources)
        println(program.sourceCode)
    }

    @Test
    fun structAsUBO() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("structAsUBO")
        println("OK")
        println(program.resources)
        println(program.sourceCode)
    }

    @Test
    fun bindlessTextures() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("bindlessTextures")
        println("OK")
        println(program.resources)
        println(program.sourceCode)
    }

    @Test
    fun ssbo() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("ssbo")
        println("OK")
        println(program.instancedInputs)
        println(program.resources)
        println(program.sourceCode)
    }

    @Test
    fun gui() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        val program = shaderCompiler.loadGLSLProgram("gui")
        println("OK")
        println(program.resources)
        println(program.sourceCode)
    }
}