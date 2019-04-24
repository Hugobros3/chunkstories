package xyz.chunkstories.graphics.vulkan.world.entities

import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class EntitiesRepresentationsProvider(val world: WorldClientCommon) : RepresentationsProvider {
    override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        for (entity in world.allLoadedEntities) {
            entity.traits[TraitRenderable::class]?.buildRepresentation(representationsGobbler)
        }

        /*val loc = Location(world, 3900.0, 51.0, 2070.0)
        val model = world.content.models["models/human/human.dae"]

        val matrix = Matrix4f()
        matrix.translate(loc.toVec3f())
        val position = ModelPosition(matrix)
        val mi = ModelInstance(model, position, animator = world.content.animationsLibrary.getAnimation("animations/human/running.bvh"))

        representationsGobbler.acceptRepresentation(mi, -1)*/
    }
}