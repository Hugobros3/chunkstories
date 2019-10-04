package xyz.chunkstories.graphics.common.world

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.region.RegionImplementation

abstract class ChunkRepresentationsProvider<R : ChunkRepresentation>(
        val world: WorldClientCommon,
        val getRepresentation: (Frame, ChunkImplementation) -> R?,
        val postGather: (Frame, List<R>) -> Unit
) : RepresentationsProvider {
    final override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        val contexts = representationsGobbler.renderTaskInstances
        val worldSize = world.worldSize
        val cameras = contexts.map { it.camera }.toTypedArray()

        val camerasSections = cameras.map {
            Pair(section(it.position.x(), world), section(it.position.z(), world))
        }.toTypedArray()

        val mainContext = contexts.find { it.name == "main" }!!
        val mainCamera = mainContext.camera

        val frame = mainContext.frame

        val visibleRegions = arrayOfNulls<RegionImplementation>(1024)
        val regionVisibility = IntArray(1024)
        var visibleRegionsCount = 0

        var rc = 0
        for (region in world.regionsManager.allLoadedRegions) {
            rc++

            var mask = 0
            for ((index, camera) in cameras.withIndex()) {
                var rx = region.regionX
                var rz = region.regionZ

                val rxSection = sectionRegion(rx, world)
                val rzSection = sectionRegion(rz, world)

                rx += shouldWrap(camerasSections[index].first, rxSection) * world.sizeInChunks / 8
                rz += shouldWrap(camerasSections[index].second, rzSection) * world.sizeInChunks / 8

                if (camera.frustrum.isBoxInFrustrum(Box.fromExtents(256.0, 256.0, 256.0).translate(rx * 256.0, region.regionY * 256.0, rz * 256.0)))
                    mask = mask or (1 shl index)
            }

            val regionVisMask = mask
            if (regionVisMask != 0) {
                regionVisibility[visibleRegionsCount] = regionVisMask
                visibleRegions[visibleRegionsCount++] = region as RegionImplementation
            }
        }

        val visibleRegionChunks = arrayOfNulls<ChunkImplementation>(8 * 8 * 8)
        val chunksVisibilityMask = IntArray(8 * 8 * 8)
        var visibleRegionChunksCount: Int

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

        val usedData = mutableListOf<R>()

        for (i in 0 until visibleRegionsCount) {
            val region = visibleRegions[i]!!
            val regionVis = regionVisibility[i]

            visibleRegionChunksCount = 0
            for (chunk in region.loadedChunks) {
                if (!chunk.isAirChunk) {

                    //if (cx in visibilityRangeX && chunk.chunkY in visibilityRangeY && cz in visibilityRangeZ) {
                        var mask = 0
                        for ((index, camera) in cameras.withIndex()) {
                            val submask = (1 shl index)
                            if (regionVis and submask == 0)
                                continue

                            var cx = chunk.chunkX
                            var cz = chunk.chunkZ

                            val cxSection = sectionChunk(cx, world)
                            val czSection = sectionChunk(cz, world)

                            cx += shouldWrap(camerasSections[index].first, cxSection) * world.sizeInChunks
                            cz += shouldWrap(camerasSections[index].second, czSection) * world.sizeInChunks

                            if (camera.frustrum.isBoxInFrustrum(Box.fromExtents(32.0, 32.0, 32.0).translate(cx * 32.0, chunk.chunkY * 32.0, cz * 32.0)))
                                mask = mask or submask
                        }
                        val chunkVisibility = mask
                        if (chunkVisibility != 0) {
                            chunksVisibilityMask[visibleRegionChunksCount] = chunkVisibility
                            visibleRegionChunks[visibleRegionChunksCount++] = chunk
                        }
                    //}
                }
            }

            gather4region(visibleRegionChunksCount, frame, visibleRegionChunks, usedData, representationsGobbler, chunksVisibilityMask, getRepresentation)
        }

        postGather(frame, usedData)
    }

    private inline fun gather4region(visibleRegionChunksCount: Int, frame: Frame, visibleRegionChunks: Array<ChunkImplementation?>,
                                     usedData: MutableList<R>, representationsGobbler: RepresentationsGobbler, chunksVisibilityMask: IntArray,
                                     getRepresentation: (Frame, ChunkImplementation) -> R?) {
        for (j in 0 until visibleRegionChunksCount) {
            //val r = obtainAndSendRepresentation(visibleRegionChunks[j]!!, chunksVisibilityMask[j])
            val r = getRepresentation(frame, visibleRegionChunks[j]!!)
            if (r != null) {
                usedData.add(r)
                representationsGobbler.acceptRepresentation(r, chunksVisibilityMask[j])
            }
        }
    }

}