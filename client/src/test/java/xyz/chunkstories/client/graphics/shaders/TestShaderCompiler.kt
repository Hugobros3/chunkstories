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

        shaderCompiler.loadGLSLProgram("simpleCase")
    }

    @Test
    fun usingStruct() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        shaderCompiler.loadGLSLProgram("usingStruct")
    }

    @Test
    fun structAsUBO() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        shaderCompiler.loadGLSLProgram("structAsUBO")
    }

    @Test
    fun gui() {
        LogbackSetupHelper.setupLoggingForTesting()
        val shaderCompiler = HeadlessShaderCompiler(GLSLDialect.VULKAN, javaClass.classLoader, null)

        shaderCompiler.loadGLSLProgram("gui")
    }
}