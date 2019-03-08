//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

/** Handles the (re)spawn point of a world  */
class SpawnCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("spawn", this)
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

        if (command.name == "spawn") {
            if (!emitter.hasPermission("world.spawn")) {
                emitter.sendMessage("You don't have the permission.")
                return true
            }

            val loc = playerEntity.world.defaultSpawnLocation
            playerEntity.location = loc

            emitter.sendMessage("#00FFD0Teleported to spawn")
            return true
        } else if (command.name == "setspawn") {
            if (!emitter.hasPermission("world.spawn.set")) {
                emitter.sendMessage("You don't have the permission.")
                return true
            }

            val loc = playerEntity.location
            playerEntity.world.defaultSpawnLocation = loc

            emitter.sendMessage("#00FFD0Set default spawn to : $loc")
            return true
        }

        return false
    }

}
