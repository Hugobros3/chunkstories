package xyz.chunkstories.graphics.vulkan.shaders

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

class VulkanShaderFactory(val backend: VulkanGraphicsBackend, val client: Client) : ShaderCompiler(GLSLDialect.VULKAN) {

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val content: Content?
        get() = client?.content

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val classLoader: ClassLoader
        //Note: We NEED that ?. operator because we're calling into this before Client is done initializing
        get() = (client?.content?.modsManager() as? ModsManagerImplementation)?.finalClassLoader ?: VulkanShaderFactory::class.java.classLoader

    fun createProgram(basePath: String, shaderCompilationParameters: ShaderCompilationParameters = ShaderCompilationParameters()) = VulkanShaderProgram(backend, loadGLSLProgram(basePath, shaderCompilationParameters))
}