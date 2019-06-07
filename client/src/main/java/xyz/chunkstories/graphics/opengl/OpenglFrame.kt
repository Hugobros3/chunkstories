package xyz.chunkstories.graphics.opengl

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.shader.ShaderResources

data class OpenglFrame constructor(override val frameNumber: Int,
                                   override val animationTimer: Float,
                                   val started: Long) : Frame {
    val stats = Stats()

    override val shaderResources = ShaderResources(null)

    class Stats {
        var totalVerticesDrawn = 0
        var totalDrawcalls = 0
    }
}