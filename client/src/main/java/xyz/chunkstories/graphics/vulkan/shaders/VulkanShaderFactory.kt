package xyz.chunkstories.graphics.vulkan.shaders

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.ShaderStage
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.GLSLProgram
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

    /*fun loadProgram(basePath: String): GLSLProgram {
        var shaderBaseDir : String? = null

        fun readShaderStage(path: String): String {
            val res = javaClass.getResource("/shaders/$path")
            if (res != null)
                return res.readText()
            val asset = client.content.getAsset("shaders/$path") ?: throw Exception("Shader not found in either built-in resources or assets: $path")
            shaderBaseDir = asset.name.substring(0, asset.name.lastIndexOf('/'))

            return asset.reader().readText()
        }

        val vertexShader = readShaderStage("$basePath.vert")
        val fragmentShader = readShaderStage("$basePath.frag")

        val stages = mapOf(ShaderStage.VERTEX to vertexShader, ShaderStage.FRAGMENT to fragmentShader)

        return try {
            translateGLSL(GLSLDialect.VULKAN, stages, client.content, shaderBaseDir)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to load program $basePath, $e")
        }
    }*/

    fun createProgram(backend: VulkanGraphicsBackend, basePath: String) = VulkanShaderProgram(backend, loadGLSLProgram(basePath))
}