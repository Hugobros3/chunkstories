package xyz.chunkstories.graphics.common.shaders.compiler

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.graphics.common.shaders.GLSLDialect

class HeadlessShaderCompiler(dialect: GLSLDialect, override val classLoader: ClassLoader, override val content: Content?) : ShaderCompiler(dialect) {

}