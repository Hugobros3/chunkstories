package xyz.chunkstories.graphics.vulkan.world.entities

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.world.WorldClientCommon

class VulkanEntitiesRenderer(val world: WorldClientCommon): RepresentationsProvider {
    override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        for(entity in world.allLoadedEntities) {
            val model = world.content.models.defaultModel

            val matrix = Matrix4f()
            matrix.translate(entity.location.toVec3f())
            val position = ModelPosition(matrix)
            val mi = ModelInstance(model, position)

            representationsGobbler.acceptRepresentation(mi, -1)
        }
    }
}