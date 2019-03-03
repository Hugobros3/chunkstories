package xyz.chunkstories.graphics.common.representations

import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderingContext
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class RepresentationsGathered(val frame: Frame,
                              val passInstances: Array<PassInstance>,
                              override val renderingContexts: Array<RenderingContext>) : RepresentationsGobbler {
    val buckets = mutableMapOf<String, Bucket>()

    inner class Bucket(val name: String) {

        val representations = arrayListOf<Representation>()
        val masks = arrayListOf<Int>()

        fun acceptRepresentation(representation: Representation, mask: Int) {
            representations.add(representation)
            masks.add(mask)
        }
    }

    override fun <T : Representation> acceptRepresentation(representation: T, camerasMask: Int) {
        val representationClassName = representation.javaClass.canonicalName ?: throw Exception("No support for anonymous Representation types !")
        val bucket = buckets.getOrPut(representationClassName) { Bucket(representationClassName) }

        bucket.acceptRepresentation(representation, camerasMask)
    }
}

fun GraphicsEngineImplementation.gatherRepresentations(frameGraph: VulkanFrameGraph, sequencedFrameGraph: List<VulkanFrameGraph.FrameGraphNode>): RepresentationsGathered {
    val passInstances: Array<PassInstance> = sequencedFrameGraph.filterIsInstance<PassInstance>().toTypedArray()
    val renderingContexts: Array<RenderingContext> = sequencedFrameGraph.filterIsInstance<RenderingContext>().toTypedArray()

    val gathered = RepresentationsGathered(frameGraph.frame, passInstances, renderingContexts)

    for (provider in backend.graphicsEngine.representationsProviders.providers) {
        provider.gatherRepresentations(gathered)
    }

    return gathered
}