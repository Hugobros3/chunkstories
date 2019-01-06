package io.xol.chunkstories.graphics.vulkan.shaders

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.graphics.ShaderStage
import io.xol.chunkstories.content.mods.ModsManagerImplementation
import io.xol.chunkstories.graphics.common.shaderc.ShaderFactory
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend

class VulkanShaderFactory(val backend: VulkanGraphicsBackend, val client: Client) : ShaderFactory(VulkanShaderFactory::class.java.classLoader) {
    override val classLoader: ClassLoader
        //Note: We NEED that ?. operator because we're calling into this before Client is done initializing
        get() = (client?.content?.modsManager() as? ModsManagerImplementation)?.finalClassLoader ?: VulkanShaderFactory::class.java.classLoader

    private fun readShaderStage(path: String): String {
        val res = javaClass.getResource("/shaders/$path")
        if (res != null)
            return res.readText()
        val asset = client.content.getAsset("shaders/$path") ?: throw Exception("Shader not found in either built-in resources or assets: $path")
        return asset.reader().readText()
    }

    fun loadProgram(basePath: String): GLSLProgram {
        val vertexShader = readShaderStage("$basePath.vert")
        val fragmentShader = readShaderStage("$basePath.frag")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return try {
            translateGLSL(GLSLDialect.VULKAN, stages)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load program $basePath, $e")
        }
    }

    fun createProgram(backend: VulkanGraphicsBackend, basePath: String) = VulkanShaderProgram(backend, loadProgram(basePath))

}