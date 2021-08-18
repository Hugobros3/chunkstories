//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import xyz.chunkstories.world.WorldImplementation

/** Handles the (re)spawn point of a world  */
class SpawnCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("spawn", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val playerEntity = emitter.entityIfIngame

        if(playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }
        val world = playerEntity.world as WorldImplementation

        if (command.name == "spawn") {
            if (!emitter.hasPermission("world.spawn")) {
                emitter.sendMessage("You don't have the permission.")
                return true
            }

            val loc = Location(world, world.properties.spawn)
            playerEntity.location = loc

            emitter.sendMessage("#00FFD0Teleported to spawn")
            return true
        } else if (command.name == "setspawn") {
            if (!emitter.hasPermission("world.spawn.set")) {
                emitter.sendMessage("You don't have the permission.")
                return true
            }

            val loc = playerEntity.location
            world.properties = world.properties.copy(spawn = loc)
            emitter.sendMessage("#00FFD0Set default spawn to : $loc")
            return true
        }

        return false
    }

}
