//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

/**
 * Spawns arbitrary entities in the World
 */
class SpawnEntityCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("spawnentity", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val playerEntity = emitter.controlledEntity

        if(playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }

        if (!emitter.hasPermission("world.spawnEntity")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }

        if (arguments.size == 0) {
            emitter.sendMessage("Syntax: /spawnEntity <entityId> [x y z]")
            return false
        }

        var spawnLocation = playerEntity.location
        if (arguments.size >= 4) {
            spawnLocation = Location(playerEntity.world, java.lang.Double.parseDouble(arguments[1]), java.lang.Double.parseDouble(arguments[2]),
                    java.lang.Double.parseDouble(arguments[3]))
        }

        val entityType: EntityDefinition?

        val TraitName = arguments[0]
        entityType = server.content.entities.getEntityDefinition(TraitName)

        if (entityType == null) {
            emitter.sendMessage("Entity type : " + arguments[0] + " not found in loaded content.")
            return true
        }

        val entity = entityType.newEntity<Entity>(spawnLocation.world)
        entity.traitLocation.set(spawnLocation)

        spawnLocation.world.addEntity(entity)

        emitter.sendMessage("#00FFD0" + "Spawned " + entity.javaClass.simpleName + " at "
                + if (arguments.size >= 4) spawnLocation.toString() else emitter.name)

        return true
    }

    companion object {

        // Lazy, why does Java standard lib doesn't have a clean way to do this tho
        // http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
        fun isNumeric(str: String): Boolean {
            for (c in str.toCharArray()) {
                if (!Character.isDigit(c))
                    return false
            }
            return true
        }
    }

}
