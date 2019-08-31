package xyz.chunkstories.graphics.common.world

import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import xyz.chunkstories.api.entity.traits.TraitAnimated
import xyz.chunkstories.api.entity.traits.TraitHitboxes
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.graphics.representation.drawCube
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider
import xyz.chunkstories.api.util.kotlin.toMatrix4d
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.world.WorldClientCommon

class EntitiesRepresentationsProvider(val world: WorldClientCommon) : RepresentationsProvider {
    override fun gatherRepresentations(representationsGobbler: RepresentationsGobbler) {
        val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
        val realWorldTimeMs = realWorldTimeTruncated / 1000_000
        val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

        for (entity in world.allLoadedEntities) {
            entity.traits[TraitRenderable::class]?.buildRepresentation(representationsGobbler)

            if (world.client.configuration.getBooleanValue(InternalClientOptions.debugWireframe)) {
                entity.traits[TraitHitboxes::class]?.let {
                    val animationTrait = entity.traits[TraitAnimated::class] ?: return
                    for (hitbox in it.hitBoxes) {
                        val fromAABBToWorld = Matrix4f()

                        fromAABBToWorld.set(animationTrait.animatedSkeleton.getBoneHierarchyTransformationMatrix(hitbox.name, animationTime ))

                        val worldPositionTransformation = Matrix4f()

                        val entityLoc = entity.location
                        val pos = Vector3f(entityLoc.x.toFloat(), entityLoc.y.toFloat(), entityLoc.z.toFloat())
                        worldPositionTransformation.translate(pos)

                        // Creates from AABB space to worldspace
                        worldPositionTransformation.mul(fromAABBToWorld, fromAABBToWorld)

                        representationsGobbler.drawCube(hitbox.box.min, hitbox.box.max, Vector4f(1.0f, 0.0f, 0.0f, 1.0f), fromAABBToWorld.toMatrix4d())
                    }
                }
            }

        }
    }
}