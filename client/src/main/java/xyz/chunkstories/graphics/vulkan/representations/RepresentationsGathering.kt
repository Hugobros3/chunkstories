package xyz.chunkstories.graphics.vulkan.representations

import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

class RepresentationsGathered(val frame: Frame) {
    val buckets = mutableMapOf<String, Bucket>()

    inner class Bucket(val name: String) {
        val representations = arrayListOf<Representation>()
        val visibility = arrayListOf<Int>()

        fun add(representation: Representation, visibilityFlags: Int) {
            representations.add(representation)
            visibility.add(visibilityFlags)
        }
    }
}

fun GraphicsEngineImplementation.gatherRepresentations(frameGraph: VulkanFrameGraph, sequencedFrameGraph: List<VulkanFrameGraph.FrameGraphNode>) : RepresentationsGathered{
    val gathered = RepresentationsGathered(frameGraph.frame)

    var currentBucket: RepresentationsGathered.Bucket? = null

    val gobbler = object: RepresentationsGobbler<Representation> {
        override val passInstances: Array<PassInstance> = sequencedFrameGraph.filterIsInstance(VulkanFrameGraph.FrameGraphNode.PassNode::class.java).toTypedArray()

        override fun acceptRepresentation(representation: Representation, cameraVisibility: Int) {
            currentBucket!!.add(representation, cameraVisibility)
        }

    }
    for(provider in backend.graphicsEngine.representationsProviders.providers) {
        val provider = provider as RepresentationsProvider<Representation>
        currentBucket = gathered.buckets.getOrPut(provider.representationName) { gathered.Bucket(provider.representationName) }
        provider.gatherRepresentations(gobbler)
    }

    return gathered
}