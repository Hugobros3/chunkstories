package xyz.chunkstories.graphics.common.shaders.compiler

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.graphics.common.shaders.GLSLDialect
import xyz.chunkstories.graphics.common.shaders.compiler.spirvcross.ResourceLocationAssigner
import xyz.chunkstories.graphics.opengl.shaders.OpenglResourceLocationAssigner

class HeadlessShaderCompiler(dialect: GLSLDialect, override val classLoader: ClassLoader, override val content: Content?) : ShaderCompiler(dialect) {
    override val newResourceLocationAssigner: () -> ResourceLocationAssigner = {
        OpenglResourceLocationAssigner()
    }
}