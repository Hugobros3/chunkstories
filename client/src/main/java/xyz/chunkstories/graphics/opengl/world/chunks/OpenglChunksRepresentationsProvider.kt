package xyz.chunkstories.graphics.opengl.world.chunks

import xyz.chunkstories.graphics.common.world.ChunkRepresentationsProvider
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.VulkanDispatchingSystem
import xyz.chunkstories.world.WorldClientCommon

class OpenglChunksRepresentationsProvider(backend: OpenglGraphicsBackend, world: WorldClientCommon) :
        ChunkRepresentationsProvider<OpenglChunkRepresentation>(world, { _, chunk ->
            if (chunk.mesh is OpenglChunkMeshProperty) {
                val block = (chunk.mesh as OpenglChunkMeshProperty).getAndAcquire()
                block
            } else {
                // This avoids the condition where the meshData is created after the chunk is destroyed
                chunk.chunkDestructionSemaphore.acquireUninterruptibly()
                if (!chunk.isDestroyed)
                    chunk.mesh = OpenglChunkMeshProperty(backend, chunk)
                chunk.chunkDestructionSemaphore.release()
                null
            }
        }, { frame, list ->

        })
