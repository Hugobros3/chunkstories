package xyz.chunkstories.graphics.common.representations

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.graphics.GraphicsEngineImplementation

class RepresentationsGathered(override val frame: Frame,
                              val passInstances: Array<PassInstance>,
                              override val renderTaskInstances: Array<RenderTaskInstance>) : RepresentationsGobbler {
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

fun GraphicsEngineImplementation.gatherRepresentations(frame: Frame, passInstances: Array<PassInstance>, renderingContexts: Array<RenderTaskInstance> ): RepresentationsGathered {
    val gathered = RepresentationsGathered(frame, passInstances, renderingContexts)

    for (provider in backend.graphicsEngine.representationsProviders.providers) {
        provider.gatherRepresentations(gathered)
    }

    return gathered
}