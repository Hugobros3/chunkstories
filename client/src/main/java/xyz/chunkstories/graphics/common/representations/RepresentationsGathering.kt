package xyz.chunkstories.graphics.common.representations

import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class RepresentationsGathered(val frame: Frame, override val passInstances: Array<PassInstance>) : RepresentationsGobbler {
    val buckets = mutableMapOf<String, Bucket>()

    inner class Bucket(val name: String) {

        val representations = arrayListOf<Representation>()
        val visibility = arrayListOf<Int>()

        fun acceptRepresentation(representation: Representation, visibilityMask: Int) {
            representations.add(representation)
            visibility.add(visibilityMask)
        }
    }

    override fun <T : Representation> acceptRepresentation(representation: T, visibilityMask: Int) {
        val representationClassName = representation.javaClass.canonicalName ?: throw Exception("No support for anonymous Representation types !")
        val bucket = buckets.getOrPut(representationClassName) { Bucket(representationClassName) }

        bucket.acceptRepresentation(representation, visibilityMask)
    }
}

fun GraphicsEngineImplementation.gatherRepresentations(frameGraph: VulkanFrameGraph, sequencedFrameGraph: List<VulkanFrameGraph.FrameGraphNode>): RepresentationsGathered {
    val passInstances: Array<PassInstance> = sequencedFrameGraph.filterIsInstance(VulkanFrameGraph.FrameGraphNode.PassNode::class.java).toTypedArray()

    val gathered = RepresentationsGathered(frameGraph.frame, passInstances)

    for (provider in backend.graphicsEngine.representationsProviders.providers) {
        provider.gatherRepresentations(gathered)
    }

    return gathered
}