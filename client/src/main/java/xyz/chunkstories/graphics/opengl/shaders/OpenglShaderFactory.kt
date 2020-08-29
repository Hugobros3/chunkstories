package xyz.chunkstories.graphics.opengl.shaders

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.content.mods.ModsManagerImplementation
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompiler
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.ResourceLocationAssigner
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

class OpenglShaderFactory(val backend: OpenglGraphicsBackend, val client: Client) : ShaderCompiler(GLSLDialect.OPENGL) {
    @Suppress("UNNECESSARY_SAFE_CALL")
    override val content: Content?
        get() = client?.content

    @Suppress("UNNECESSARY_SAFE_CALL")
    override val classLoader: ClassLoader
        //Note: We NEED that ?. operator because we're calling into this before Client is done initializing
        get() = (client?.content?.modsManager as? ModsManagerImplementation)?.finalClassLoader ?: OpenglShaderFactory::class.java.classLoader

    override val newResourceLocationAssigner: () -> ResourceLocationAssigner = {
        OpenglResourceLocationAssigner()
    }

    fun createProgram(basePath: String, shaderCompilationParameters: ShaderCompilationParameters = ShaderCompilationParameters()) = OpenglShaderProgram(backend, loadGLSLProgram(basePath, shaderCompilationParameters))

    override val spirv_13: Boolean = false
}
