package io.xol.chunkstories.graphics.vulkan.shaderc

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.content.mods.ModsManagerImplementation
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper

class VulkanShaderFactory(val client: Client) : ShaderFactory(VulkanShaderFactory::class.java.classLoader) {
    override val classLoader: ClassLoader
        get() = (client.content.modsManager() as ModsManagerImplementation).finalClassLoader!!

    fun loadProgram(basePath: String): GLSLProgram {
        val vertexShader = javaClass.getResource("$basePath.vert").readText()
        val fragmentShader = javaClass.getResource("$basePath.frag").readText()

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return try { translateGLSL(GLSLDialect.VULKAN, stages) } catch(e: Exception) { throw Exception("Failed to load program $basePath, $e") }
    }

    fun createShaderProgram(basePath: String): VulkanicShaderProgram {
        return VulkanicShaderProgram(loadProgram(basePath))
    }

    data class VulkanicShaderProgram(val glslProgram: GLSLProgram) {
        val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram)

        init {

        }

        fun cleanup() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

enum class VulkanShaderUniformResourceType {
    UNIFORM_BLOCK,
    SAMPLER2D
}