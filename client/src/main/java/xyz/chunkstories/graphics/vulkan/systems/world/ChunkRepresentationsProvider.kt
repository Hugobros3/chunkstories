package xyz.chunkstories.graphics.vulkan.systems.world

import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.storage.RegionImplementation

class ChunkRepresentationsProvider(val backend: VulkanGraphicsBackend, val world: WorldClientCommon) : RepresentationsProvider {
    override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        //val passes = representationsGobbler.con
        val contexts=  representationsGobbler.renderingContexts
        val cameras = contexts.map { it.camera }

        //val mainPass = passes.find { it.context.name == "main" }!! as VulkanFrameGraph.FrameGraphNode.PassNode
        //val mainContext = mainPass.context
        val mainContext = contexts.find { it.name == "main" }!! as VulkanFrameGraph.FrameGraphNode.RenderingContextNode
        val mainCamera = mainContext.camera

        val frame = mainContext.frameGraph.frame

        fun getVisibility(boxCenter: Vector3fc, boxSize: Vector3fc): Int {
            var mask = 0
            for((index, camera) in cameras.withIndex()) {
                if(camera.frustrum.isBoxInFrustrum(boxCenter, boxSize))
                    mask = mask or (1 shl index)
            }
            return mask
        }

        fun refineVisibility(boxCenter: Vector3fc, boxSize: Vector3fc, previousMask: Int): Int {
            var mask = 0
            for((index, camera) in cameras.withIndex()) {
                val submask = (1 shl index)
                if(previousMask and submask == 0)
                    continue

                if(camera.frustrum.isBoxInFrustrum(boxCenter, boxSize))
                    mask = mask or submask
            }
            return mask
        }

        val boxCenter = Vector3f(0f)
        val boxSize = Vector3f(32f, 32f, 32f)
        val boxSize2 = Vector3f(256f, 256f, 256f)

        val visibleRegions = arrayOfNulls<RegionImplementation>(1024)
        val regionVisibility = IntArray(1024)
        var visibleRegionsCount = 0

        var rc = 0
        for (region in world.allLoadedRegions) {
            boxCenter.x = region.regionX * 256.0f + 128.0f
            boxCenter.y = region.regionY * 256.0f + 128.0f
            boxCenter.z = region.regionZ * 256.0f + 128.0f

            rc++

            val regionVisMask = getVisibility(boxCenter, boxSize2)
            if(regionVisMask != 0) {
                regionVisibility[visibleRegionsCount] = regionVisMask
                visibleRegions[visibleRegionsCount++] = region as RegionImplementation
            }
        }

        /*Arrays.sort(visibleRegions, 0, visibleRegionsCount) { a, b ->
            fun distSquared(r: Region) : Float {
                val rcx = r.regionX * 256.0f + 128.0f
                val rcy = r.regionY * 256.0f + 128.0f
                val rcz = r.regionZ * 256.0f + 128.0f

                val dx = camPos.x() - rcx
                val dy = camPos.y() - rcy
                val dz = camPos.z() - rcz

                return dx * dx + dy * dy + dz * dz
            }

            (distSquared(a!!) - distSquared(b!!)).toInt()
        }*/

        val visibleRegionChunks = arrayOfNulls<CubicChunk>(8 * 8 * 8)
        val chunksVisibilityMask = IntArray(8 * 8 * 8)
        var visibleRegionChunksCount : Int

        val camPos = mainCamera.position

        val camChunk = camPos.toVec3i()
        camChunk.x /= 32
        camChunk.y /= 32
        camChunk.z /= 32

        val drawDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
        val drawDistanceH = 6

        val visibilityRangeX = (camChunk.x - drawDistance)..(camChunk.x + drawDistance)
        val visibilityRangeY = (camChunk.y - drawDistanceH)..(camChunk.y + drawDistanceH)
        val visibilityRangeZ = (camChunk.z - drawDistance)..(camChunk.z + drawDistance)

        val usedData = mutableListOf<ChunkRepresentation>()

        fun obtainAndSendRepresentation(chunk: CubicChunk, visibility: Int) {
            if (chunk.meshData is VulkanChunkMeshProperty) {
                val block = (chunk.meshData as VulkanChunkMeshProperty).get()
                if (block != null) {
                    usedData.add(block)
                    representationsGobbler.acceptRepresentation(block, visibility)
                }
            } else {
                // This avoids the condition where the meshData is created after the chunk is destroyed
                chunk.chunkDestructionSemaphore.acquireUninterruptibly()
                if (!chunk.isDestroyed)
                    chunk.meshData = VulkanChunkMeshProperty(backend, chunk)
                chunk.chunkDestructionSemaphore.release()
            }
        }

        for(i in 0 until visibleRegionsCount) {
            val region = visibleRegions[i]!!
            val regionVis = regionVisibility[i]

            visibleRegionChunksCount = 0
            for (chunk in region.loadedChunks) {
                boxCenter.x = chunk.chunkX * 32.0f + 16.0f
                boxCenter.y = chunk.chunkY * 32.0f + 16.0f
                boxCenter.z = chunk.chunkZ * 32.0f + 16.0f

                if(!chunk.isAirChunk) {
                    if(chunk.chunkX in visibilityRangeX && chunk.chunkY in visibilityRangeY && chunk.chunkZ in visibilityRangeZ) {

                        val chunkVisibility = refineVisibility(boxCenter, boxSize, regionVis)
                        if (chunkVisibility != 0) {
                            chunksVisibilityMask[visibleRegionChunksCount] = chunkVisibility
                            visibleRegionChunks[visibleRegionChunksCount++] = chunk
                        }
                    }
                }
            }

            /*Arrays.sort(visibleRegionChunks, 0, visibleRegionChunksCount) { a, b ->
                fun distSquared(c: Chunk) : Float {
                    val ccx = c.chunkX * 32.0f + 16.0f
                    val ccy = c.chunkY * 32.0f + 16.0f
                    val ccz = c.chunkZ * 32.0f + 16.0f

                    val dx = camPos.x() - ccx
                    val dy = camPos.y() - ccy
                    val dz = camPos.z() - ccz

                    return dx * dx + dy * dy + dz * dz
                }

                (distSquared(a!!) - distSquared(b!!)).toInt()
            }*/

            for(j in 0 until visibleRegionChunksCount) {
                obtainAndSendRepresentation(visibleRegionChunks[j]!!, chunksVisibilityMask[j])
            }
        }

        frame.recyclingTasks.add {
            usedData.forEach(ChunkRepresentation::release)
        }
    }

}