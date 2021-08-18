package xyz.chunkstories.graphics.vulkan.systems.dispatching.chunks

import xyz.chunkstories.graphics.common.world.ChunkRepresentationsProvider
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.world.WorldImplementation

class VulkanChunkRepresentationsProvider(val backend: VulkanGraphicsBackend, world: WorldImplementation) :
        ChunkRepresentationsProvider<VulkanChunkRepresentation>(world, { _, chunk ->
            if (chunk.mesh is VulkanChunkMeshProperty) {
                val block = (chunk.mesh as VulkanChunkMeshProperty).getAndAcquire()
                block
            } else {
                // This avoids the condition where the meshData is created after the chunk is destroyed
                chunk.chunkDestructionSemaphore.acquireUninterruptibly()
                if (!chunk.isDestroyed)
                    chunk.mesh = VulkanChunkMeshProperty(backend, chunk)
                chunk.chunkDestructionSemaphore.release()
                null
            }
        }, { frame, list ->
            (frame as VulkanFrame).recyclingTasks.add {
                list.forEach(VulkanChunkRepresentation::release)
            }
        })
