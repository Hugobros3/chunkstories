package xyz.chunkstories.graphics.common.representations

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.rendergraph.PassInstance
import xyz.chunkstories.api.graphics.rendergraph.RenderTaskInstance
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.graphics.GraphicsEngineImplementation

class RepresentationsGathered(override val frame: Frame,
                              override val renderTaskInstances: Array<RenderTaskInstance>) : RepresentationsGobbler {

    val buckets = mutableMapOf<Class<Any?>, Buckets>()

    inner class Buckets(val representationName: String, val maskedBuckets: MutableMap<Int, Bucket> = mutableMapOf())

    inner class Bucket(val mask: Int) {
        val representations = arrayListOf<Representation>()
    }

    override fun <T : Representation> acceptRepresentation(representation: T, mask: Int) {
        val rclass = representation.javaClass
        val bucket = buckets.getOrPut(rclass as Class<Any?>) {

            val representationClassName = rclass.canonicalName ?: throw Exception("No support for anonymous Representation types !")
            Buckets(representationClassName)
        }.maskedBuckets.getOrPut(mask) {
            Bucket(mask)
        }

        bucket.representations.add(representation)
    }

    override fun <T : Representation> acceptRepresentation(representation: T) {
        acceptRepresentation(representation, -1)
    }
}

fun GraphicsEngineImplementation.gatherRepresentations(frame: Frame, passInstances: List<PassInstance>, renderTasks: List<RenderTaskInstance> ): RepresentationsGathered {
    val gathered = RepresentationsGathered(frame, renderTasks.toTypedArray())

    for (provider in backend.graphicsEngine.representationsProviders.providers) {
        provider.gatherRepresentations(gathered)
    }

    return gathered
}