package io.xol.chunkstories.graphics.vulkan.shaderc

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.content.mods.ModsManagerImplementation
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper
import io.xol.chunkstories.graphics.vulkan.ShaderModule
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend

class VulkanShaderFactory(val client: Client) : ShaderFactory(VulkanShaderFactory::class.java.classLoader) {
    override val classLoader: ClassLoader
        get() = (client.content.modsManager() as ModsManagerImplementation).finalClassLoader!!

    fun loadProgram(basePath: String): GLSLProgram {
        val vertexShader = javaClass.getResource("$basePath.vert").readText()
        val fragmentShader = javaClass.getResource("$basePath.frag").readText()

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return try { translateGLSL(GLSLDialect.VULKAN, stages) } catch(e: Exception) { throw Exception("Failed to load program $basePath, $e") }
    }

    data class VulkanicShaderProgram(val backend: VulkanGraphicsBackend, val glslProgram: GLSLProgram) {
        val spirvCode = SpirvCrossHelper.generateSpirV(glslProgram)
        val modules: Map<ShaderStage, ShaderModule>

        init {
            modules = mapOf(*spirvCode.stages.map { (stage, byteBuffer) -> Pair(stage, ShaderModule(backend, byteBuffer))}.toTypedArray() )
        }

        fun cleanup() {
            modules.values.forEach { it.cleanup() }
        }
    }
}

enum class VulkanShaderUniformResourceType {
    UNIFORM_BLOCK,
    SAMPLER2D
}