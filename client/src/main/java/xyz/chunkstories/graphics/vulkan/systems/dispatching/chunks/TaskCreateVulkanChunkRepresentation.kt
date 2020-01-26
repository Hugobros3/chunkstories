package xyz.chunkstories.graphics.vulkan.systems.dispatching.chunks

import xyz.chunkstories.graphics.common.world.TaskCreateChunkMesh
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.common.voxel.VoxelTexturesArray
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import java.nio.ByteBuffer

class TaskCreateVulkanChunkRepresentation(val backend: VulkanGraphicsBackend, chunk: ChunkImplementation, attachedProperty: AutoRebuildingProperty, updates: Int) :
        TaskCreateChunkMesh(chunk, attachedProperty, updates
                , {
            (it as VoxelTexturesArray.VoxelTextureInArray).textureArrayIndex
        }
                , { sections ->
            val sectionsBuffers = sections.mapValues {
                val scratch = it.value

                fun buf2vk(buffer: ByteBuffer): VulkanVertexBuffer {
                    buffer.flip()
                    val vertexBuffer = VulkanVertexBuffer(backend, buffer.limit().toLong(), MemoryUsagePattern.SEMI_STATIC)
                    vertexBuffer.upload(buffer)
                    return vertexBuffer
                }

                val cubes = if (scratch.cubesCount > 0) VulkanChunkRepresentation.Section.CubesInstances(buf2vk(scratch.cubesData), scratch.cubesCount) else null
                val staticMesh = if (scratch.meshTriCount > 0) VulkanChunkRepresentation.Section.StaticMesh(buf2vk(scratch.meshData), scratch.meshTriCount) else null

                VulkanChunkRepresentation.Section(it.key, cubes, staticMesh)
            }

            val receiver = chunk.mesh as VulkanChunkMeshProperty
            receiver.acceptNewData(sectionsBuffers)
        })