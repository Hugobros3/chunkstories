package xyz.chunkstories.graphics.opengl.world.chunks

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.world.ChunkRepresentation
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.world.chunk.CubicChunk

class OpenglChunkRepresentation(override val chunk: CubicChunk, val sections: Map<String, Section>
) : ChunkRepresentation, Cleanable {

    class Section(val materialTag: String, val cubes: CubesInstances?, val staticMesh: StaticMesh?) {
        lateinit var parent: OpenglChunkRepresentation

        data class StaticMesh(val buffer: OpenglVertexBuffer, val count: Int) {
            lateinit var parent: OpenglChunkRepresentation
        }

        data class CubesInstances(val buffer: OpenglVertexBuffer, val count: Int) {
            lateinit var parent: OpenglChunkRepresentation
        }
    }

    init {
        sections.values.forEach {
            it.parent = this
            it.cubes?.parent = this
            it.staticMesh?.parent = this
        }
    }

    override fun cleanup() {
        sections.values.forEach {
            it.cubes?.buffer?.cleanup()
            it.staticMesh?.buffer?.cleanup()
        }
    }
}