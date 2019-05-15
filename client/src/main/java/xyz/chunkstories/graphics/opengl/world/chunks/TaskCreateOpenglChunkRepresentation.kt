package xyz.chunkstories.graphics.opengl.world.chunks

import xyz.chunkstories.graphics.common.voxel.VoxelTexturesArray
import xyz.chunkstories.graphics.common.world.TaskCreateChunkMesh
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import java.nio.ByteBuffer

class TaskCreateVulkanChunkRepresentation(val backend: OpenglGraphicsBackend, chunk: CubicChunk, attachedProperty: AutoRebuildingProperty, updates: Int) :
        TaskCreateChunkMesh(chunk, attachedProperty, updates
                , {
            (it as VoxelTexturesArray.VoxelTextureInArray).textureArrayIndex
        }
                , { sections ->

            backend.window.mainThreadBlocking {
                val sectionsBuffers = sections.mapValues {
                    val scratch = it.value

                    fun buf2vk(buffer: ByteBuffer): OpenglVertexBuffer {
                        buffer.flip()
                        val vertexBuffer = OpenglVertexBuffer(backend)
                        vertexBuffer.upload(buffer)
                        return vertexBuffer
                    }

                    val cubes = if (scratch.cubesCount > 0) OpenglChunkRepresentation.Section.CubesInstances(buf2vk(scratch.cubesData), scratch.cubesCount) else null
                    val staticMesh = if (scratch.meshTriCount > 0) OpenglChunkRepresentation.Section.StaticMesh(buf2vk(scratch.meshData), scratch.meshTriCount) else null

                    OpenglChunkRepresentation.Section(it.key, cubes, staticMesh)
                }

                val receiver = chunk.mesh as OpenglChunkMeshProperty
                receiver.acceptNewData(sectionsBuffers)
            }
        })