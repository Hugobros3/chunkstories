package io.xol.chunkstories.graphics.vulkan.shaderc

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.content.mods.ModsManagerImplementation
import io.xol.chunkstories.graphics.common.shaderc.InterfaceBlockGLSLMapping
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.common.shaderc.SpirvCrossHelper
import java.nio.ByteBuffer

class VulkanShaderFactory(val client: Client) : ShaderFactory(VulkanShaderFactory::class.java.classLoader) {
    override val classLoader: ClassLoader
        get() = (client.content.modsManager() as ModsManagerImplementation).finalClassLoader!!

    fun loadProgram(basePath: String): SpirvCrossHelper.GeneratedSpirV {
        val vertexShader = javaClass.getResource("$basePath.vert").readText()
        val fragmentShader = javaClass.getResource("$basePath.frag").readText()

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return translateGLSL(GLSLDialect.VULKAN, stages)?.let { SpirvCrossHelper.generateSpirV(it) } ?: throw Exception("Failed to load program $basePath")
    }

    data class VulkanicShaderProgram(val stages: Map<ShaderStage, ByteBuffer>, val uniformBlocks: List<VulkanShaderProgramUniformBlock>)

    data class VulkanShaderProgramUniformBlock(val name: String, val set: Int, val binding: Int, val mapper: InterfaceBlockGLSLMapping)
}