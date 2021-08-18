//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

class TimeCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("time", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (!emitter.hasPermission("world.time")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val playerEntity = emitter.entityIfIngame

        if(playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }

        when {
            arguments.isEmpty() -> emitter.sendMessage("#82FFDBCurrent time is: ${playerEntity.location.world.sky.timeOfDay}")
            arguments.size == 1 -> {
                val newTime = arguments[0].toInt()
                playerEntity.location.world.apply {
                    sky = sky.copy(timeOfDay = newTime / 24000.0f)
                }
                emitter.sendMessage("#82FFDBSet time to  :$newTime")
            }
            else -> emitter.sendMessage("#82FFDBSyntax : /time [0-24000]")
        }
        return true
    }

}
