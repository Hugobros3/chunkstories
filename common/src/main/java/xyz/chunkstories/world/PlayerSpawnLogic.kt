package xyz.chunkstories.world

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.EntitySerialization
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitName
import xyz.chunkstories.api.events.player.PlayerSpawnEvent
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster

fun WorldCommon.figureOutWherePlayerWillSpawn(player: Player): Location {
    val playerWorldMetadata = this.playersMetadata[player.id]!!
    val savedEntity = playerWorldMetadata.savedEntity?.let { EntitySerialization.deserializeEntity(this, it) }

    var previousLocation: Location? = null
    if (savedEntity != null) {
        previousLocation = savedEntity.location
        println("has previous: $savedEntity")
    }

    val playerSpawnEvent = PlayerSpawnEvent(player, this as WorldMaster, savedEntity, previousLocation)
    this.gameInstance.pluginManager.fireEvent(playerSpawnEvent)

    //entity = playerSpawnEvent.entity
    var expectedSpawnLocation = playerSpawnEvent.spawnLocation
    if (expectedSpawnLocation == null)
        expectedSpawnLocation = Location(this, this.properties.spawn)

    return expectedSpawnLocation
}

fun WorldCommon.spawnPlayer(player: Player, force: Boolean = false) {
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

        val shouldSpawnAtLocation = playerSpawnEvent.spawnLocation ?: this.defaultSpawnLocation

        // Create a new entity if the event didn't handle it
        if (entity == null || entity.traits[TraitHealth::class.java]?.isDead == true) {
            entity = this.gameContext.content.entities.getEntityDefinition("player")!!.newEntity(this)
        }

        // Spawn point we checked as "valid" (ie not inside a block)
        var validatedSpawnLocation = Location(shouldSpawnAtLocation)

        val traitCollisions = entity.traits[TraitCollidable::class]
        if (traitCollisions != null) {
            fun isSuitableSpawningPoint(location: Location): Boolean {
                val voxel = peek(location).voxel
                var collision = false
                for (box in traitCollisions.collisionBoxes) {
                    box.translate(location)
                    if (box.collidesWith(this))
                        collision = true
                }

                return !collision && !voxel.liquid
            }

            if (!isSuitableSpawningPoint(validatedSpawnLocation)) {
                // Try 10 times to spawn
                for (i in 0..9) {
                    val attemptedSpawnLocation = Location(shouldSpawnAtLocation)
                    attemptedSpawnLocation.y = 0.0

                    // After the first attempt, start fuzzing the spawn location
                    if (i > 0) {
                        shouldSpawnAtLocation.x += (Math.random() - 0.5) * 2.0 * i * 32
                        shouldSpawnAtLocation.z += (Math.random() - 0.5) * 2.0 * i * 32
                    }

                    var lastGoodSpawnLocation: Location? = null
                    var lastWasSuitable = false

                    // Look for correct spawn points all the way to the top
                    while (attemptedSpawnLocation.y < worldInfo.size.heightInChunks * 32) {
                        val suitable = isSuitableSpawningPoint(attemptedSpawnLocation)

                        if (suitable) {
                            if (!lastWasSuitable) {
                                lastGoodSpawnLocation = Location(attemptedSpawnLocation)
                            }
                            lastWasSuitable = true
                        } else {
                            lastWasSuitable = false
                        }

                        attemptedSpawnLocation.y += 1
                    }

                    if (lastGoodSpawnLocation != null) {
                        validatedSpawnLocation = Location(lastGoodSpawnLocation)
                        break
                    }
                }
            }
        }

        entity.traits[TraitName::class.java]?.name = player.name
        entity.traitLocation.set(validatedSpawnLocation)

        this.addEntity(entity)
        player.controlledEntity = entity

        playerWorldMetadata.savedEntity = null
    }
}