//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

class WeatherCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("weather", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (!emitter.hasPermission("world.weather")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val playerEntity = emitter.controlledEntity

        if(playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }

        when {
            arguments.isEmpty() -> {
                emitter.sendMessage("#82FFDBCurrent weather is ${playerEntity.location.world.weather}")
            }
            arguments.size == 1 -> {
                val overcastFactor = java.lang.Float.parseFloat(arguments[0])
                playerEntity.location.world.weather = overcastFactor
                emitter.sendMessage("#82FFDBSet weather for world to $overcastFactor")
            }
            else -> emitter.sendMessage("#82FFDBSyntax : /weather [0.0 - 1.0]")
        }
        return true
    }

}
