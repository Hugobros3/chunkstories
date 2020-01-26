package xyz.chunkstories.graphics.common.shaders.compiler

import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.graphics.rendergraph.PassOutputsDeclaration

data class ShaderCompilationParameters(val outputs: PassOutputsDeclaration? = null, val inputs: List<AvailableVertexInput>? = null, val defines: Map<String, String> = emptyMap())

data class AvailableVertexInput(val name: String, val components: Int, val format: VertexFormat)