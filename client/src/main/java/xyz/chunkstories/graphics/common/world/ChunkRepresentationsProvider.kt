package xyz.chunkstories.graphics.common.world

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.Frustrum
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

                if (camera.frustrum.isBoxInFrustrumFAST(Box.fromExtents(256.0, 256.0, 256.0).translate(rx * 256.0, region.regionY * 256.0, rz * 256.0)))
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

        val usedData = mutableListOf<R>()

        for (i in 0 until visibleRegionsCount) {
            val region = visibleRegions[i]!!
            val regionVis = regionVisibility[i]

            visibleRegionChunksCount = 0
            for (chunk in region.loadedChunks) {
                if (!chunk.isAirChunk) {

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

                        if (camera.frustrum.isBoxInFrustrumFAST(Box.fromExtents(32.0, 32.0, 32.0).translate(cx * 32.0, chunk.chunkY * 32.0, cz * 32.0)))
                            mask = mask or submask
                    }
                    val chunkVisibility = mask
                    if (chunkVisibility != 0) {
                        chunksVisibilityMask[visibleRegionChunksCount] = chunkVisibility
                        visibleRegionChunks[visibleRegionChunksCount++] = chunk
                    }
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

    private inline fun Frustrum.isBoxInFrustrumFAST(box: Box): Boolean {
        //return this.isBoxInFrustrum(box)
        return this.isBoxInFrustrumFAST(box.min, box.max)
    }

    private fun Frustrum.isBoxInFrustrumFAST(min: Vector3dc, max: Vector3dc): Boolean {
        val v0 = Vector3d(
                // i=0 j=0 k=0
                min.x(),
                min.y(),
                min.z()
        )

        val v1 = Vector3d(
                // i=0 j=0 k=1
                min.x(),
                min.y(),
                max.z()
        )

        val v2 = Vector3d(
                // i=0 j=1 k=0
                min.x(),
                max.x(),
                min.z()
        )

        val v3 = Vector3d(
                // i=0 j=1 k=1
                min.x(),
                max.y(),
                max.z()
        )

        val v4 = Vector3d(
                // i=1 j=0 k=0
                max.x(),
                min.y(),
                min.z()
        )
        val v5 = Vector3d(
                // i=1 j=0 k=1
                max.x(),
                min.y(),
                max.z()
        )
        val v6 = Vector3d(
                // i=1 j=1 k=0
                max.x(),
                max.y(),
                min.z()
        )
        val v7 = Vector3d(
                // i=1 j=1 k=1
                max.x(),
                max.y(),
                max.z()
        )

        for (i in 4 downTo 0) {
            var inside = 0
            /*for (c in 0..7) {
                val corner = when (c) {
                    0 -> v0
                    1 -> v1
                    2 -> v2
                    3 -> v3
                    4 -> v4
                    5 -> v5
                    6 -> v6
                    else -> v7
                }
                val wtf = cameraPlanes[i].distance(corner)

                //if (wtf.isNaN() || wtf.isInfinite()) {
                //    println("wow $i ${wtf.isInfinite()} ${wtf.isNaN()} $min $max")
                //    println("${cameraPlanes[i]}")
                //}
                if (!(cameraPlanes[i].distance(corner) < 0)) {
                    inside++
                }
            }*/
            if(cameraPlanes[i].distance(v0) >= 0)
                inside++
            if(cameraPlanes[i].distance(v1) >= 0)
                inside++
            if(cameraPlanes[i].distance(v2) >= 0)
                inside++
            if(cameraPlanes[i].distance(v3) >= 0)
                inside++
            if(cameraPlanes[i].distance(v4) >= 0)
                inside++
            if(cameraPlanes[i].distance(v5) >= 0)
                inside++
            if(cameraPlanes[i].distance(v6) >= 0)
                inside++
            if(cameraPlanes[i].distance(v7) >= 0)
                inside++

            if (inside == 0) {
                return false
            }
        }
        return true
    }

}