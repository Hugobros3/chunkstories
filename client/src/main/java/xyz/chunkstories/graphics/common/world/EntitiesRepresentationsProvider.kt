package xyz.chunkstories.graphics.common.world

import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class EntitiesRepresentationsProvider(val world: WorldClientCommon) : RepresentationsProvider {
    override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        for (entity in world.allLoadedEntities) {
            entity.traits[TraitRenderable::class]?.buildRepresentation(representationsGobbler)
        }
    }
}