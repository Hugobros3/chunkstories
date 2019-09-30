package xyz.chunkstories.world

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitName
import xyz.chunkstories.api.events.player.PlayerSpawnEvent
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster

fun WorldImplementation.figureOutWherePlayerWillSpawn(player: Player): Location {
    val playerWorldMetadata = this.playersMetadata[player]!!
    val savedEntity = playerWorldMetadata.savedEntity?.let { EntitySerialization.deserializeEntity(this, it) }

    var previousLocation: Location? = null
    if (savedEntity != null) {
        previousLocation = savedEntity.location
        println("has previous: $savedEntity")
    }

    val playerSpawnEvent = PlayerSpawnEvent(player, this as WorldMaster, savedEntity, previousLocation)
    this.gameContext.pluginManager.fireEvent(playerSpawnEvent)

    //entity = playerSpawnEvent.entity
    var expectedSpawnLocation = playerSpawnEvent.spawnLocation
    if (expectedSpawnLocation == null)
        expectedSpawnLocation = this.defaultSpawnLocation

    return expectedSpawnLocation
}

fun WorldImplementation.spawnPlayer(player: Player, force: Boolean = false) {
    if (this !is WorldMaster)
        throw UnsupportedOperationException("Only Master Worlds can do this")

    val playerWorldMetadata = playersMetadata[player]!!
    val savedEntity = playerWorldMetadata.savedEntity?.let { EntitySerialization.deserializeEntity(this, it) }

    var previousLocation: Location? = null
    if (savedEntity != null) {
        previousLocation = savedEntity.location
    }

    val playerSpawnEvent = PlayerSpawnEvent(player, this as WorldMaster, savedEntity, previousLocation)
    gameContext.pluginManager.fireEvent(playerSpawnEvent)

    if (!playerSpawnEvent.isCancelled || force) {
        var entity = playerSpawnEvent.entity

        var actualSpawnLocation = playerSpawnEvent.spawnLocation
        if (actualSpawnLocation == null)
            actualSpawnLocation = this.defaultSpawnLocation

        if (entity == null || entity.traits[TraitHealth::class.java]?.isDead == true) {
            entity = this.gameContext.content.entities.getEntityDefinition("player")!!.newEntity(this)
        }


        var freeSpawnLocation = Location(actualSpawnLocation)
        val traitCollisions = entity.traits[TraitCollidable::class]
        if (traitCollisions != null) {
            // Try 10 times to spawn
            for(i in 0..9) {
                freeSpawnLocation = Location(actualSpawnLocation)

                var lastVoxel = content.voxels.air
                while (true) {
                    var collision = false
                    for (box in traitCollisions.collisionBoxes) {
                        box.translate(freeSpawnLocation)
                        if (box.collidesWith(this))
                            collision = true
                    }

                    if (!collision)
                        break

                    val voxel = peek(freeSpawnLocation).voxel
                    if(!voxel.isAir())
                        lastVoxel = voxel

                    freeSpawnLocation = Location(freeSpawnLocation)
                    freeSpawnLocation.y += 1
                }

                // Success: this won't spawn you in a liquid
                if(!lastVoxel.liquid)
                    break
            }
        }

        entity.traits[TraitName::class.java]?.name = player.name
        entity.traitLocation.set(freeSpawnLocation)

        this.addEntity(entity)
        player.controlledEntity = entity

        playerWorldMetadata.savedEntity = null
    }
}