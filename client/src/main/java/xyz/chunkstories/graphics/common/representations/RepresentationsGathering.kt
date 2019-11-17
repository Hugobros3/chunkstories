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

    val buckets = mutableMapOf<Class<Any?>, Bucket>()

    inner class Bucket(val representationName: String) {
        val representations = arrayListOf<Representation>()
        val masks = arrayListOf<Int>()

        fun acceptRepresentation(representation: Representation, mask: Int) {
            representations.add(representation)
            masks.add(mask)
        }
    }

    override fun <T : Representation> acceptRepresentation(representation: T, mask: Int) {
        val rclass = representation.javaClass
        val bucket = buckets.getOrPut(rclass as Class<Any?>) {
            val representationClassName = rclass.canonicalName ?: throw Exception("No support for anonymous Representation types !")
            Bucket(representationClassName)
        }

        bucket.acceptRepresentation(representation, mask)
    }
}

fun GraphicsEngineImplementation.gatherRepresentations(frame: Frame, passInstances: Array<PassInstance>, renderingContexts: Array<RenderTaskInstance> ): RepresentationsGathered {
    val gathered = RepresentationsGathered(frame, passInstances, renderingContexts)

    for (provider in backend.graphicsEngine.representationsProviders.providers) {
        provider.gatherRepresentations(gathered)
    }

    return gathered
}