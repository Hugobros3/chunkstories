package xyz.chunkstories.graphics.common.world

import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.util.kotlin.toVec3i
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.world.WorldClientCommon
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.storage.RegionImplementation

abstract class ChunkRepresentationsProvider<R : ChunkRepresentation>(
        val world: WorldClientCommon,
        val getRepresentation: (Frame, CubicChunk) -> R?,
        val postGather: (Frame, List<R>) -> Unit
) : RepresentationsProvider {
    final override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        val contexts = representationsGobbler.renderTaskInstances
        val cameras = contexts.map { it.camera }

        val mainContext = contexts.find { it.name == "main" }!!
        val mainCamera = mainContext.camera

        val frame = mainContext.frame

        fun getVisibility(box: Box): Int {
            var mask = 0
            for ((index, camera) in cameras.withIndex()) {
                if (camera.frustrum.isBoxInFrustrum(box))
                    mask = mask or (1 shl index)
            }
            return mask
        }

        fun refineVisibility(box: Box, previousMask: Int): Int {
            var mask = 0
            for ((index, camera) in cameras.withIndex()) {
                val submask = (1 shl index)
                if (previousMask and submask == 0)
                    continue

                if (camera.frustrum.isBoxInFrustrum(box))
                    mask = mask or submask
            }
            return mask
        }

        //val min = Vector3f(0f)
        //val boxSize = Vector3f(32f, 32f, 32f)
        //val max = Vector3f(256f, 256f, 256f)

        val visibleRegions = arrayOfNulls<RegionImplementation>(1024)
        val regionVisibility = IntArray(1024)
        var visibleRegionsCount = 0

        var rc = 0
        for (region in world.regionsManager.allLoadedRegions) {
            /*min.x = region.regionX * 256.0f + 0.0f
            min.y = region.regionY * 256.0f + 0.0f
            min.z = region.regionZ * 256.0f + 0.0f

            max.x = region.regionX * 256.0f + 256.0f
            max.y = region.regionY * 256.0f + 256.0f
            max.z = region.regionZ * 256.0f + 256.0f*/

            rc++

            val regionVisMask = getVisibility(Box.fromExtents(256.0, 256.0, 256.0).translate(region.regionX * 256.0, region.regionY * 256.0, region.regionZ * 256.0))
            if (regionVisMask != 0) {
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

        /*fun obtainAndSendRepresentation(chunk: CubicChunk): R? {

        }*/

        for (i in 0 until visibleRegionsCount) {
            val region = visibleRegions[i]!!
            val regionVis = regionVisibility[i]

            visibleRegionChunksCount = 0
            for (chunk in region.loadedChunks) {
                /*min.x = chunk.chunkX * 32.0f + 0.0f
                min.y = chunk.chunkY * 32.0f + 0.0f
                min.z = chunk.chunkZ * 32.0f + 0.0f

                max.x = chunk.chunkX * 32.0f + 32.0f
                max.y = chunk.chunkY * 32.0f + 32.0f
                max.z = chunk.chunkZ * 32.0f + 32.0f*/

                if (!chunk.isAirChunk) {
                    if (chunk.chunkX in visibilityRangeX && chunk.chunkY in visibilityRangeY && chunk.chunkZ in visibilityRangeZ) {

                        val chunkVisibility = refineVisibility(Box.fromExtents(32.0, 32.0, 32.0).translate(chunk.chunkX * 32.0, chunk.chunkY * 32.0, chunk.chunkZ * 32.0), regionVis)
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

            gather4region(visibleRegionChunksCount, frame, visibleRegionChunks, usedData, representationsGobbler, chunksVisibilityMask, getRepresentation)
        }

        postGather(frame, usedData)
    }

    private inline fun gather4region(visibleRegionChunksCount: Int, frame: Frame, visibleRegionChunks: Array<CubicChunk?>,
                                     usedData: MutableList<R>, representationsGobbler: RepresentationsGobbler, chunksVisibilityMask: IntArray,
                                     getRepresentation: (Frame, CubicChunk) -> R?) {
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