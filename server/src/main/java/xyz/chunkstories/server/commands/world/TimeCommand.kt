//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.world

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

class TimeCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("time", this)
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

        val playerEntity = emitter.controlledEntity

        if(playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }

        when {
            arguments.isEmpty() -> emitter.sendMessage("#82FFDBCurrent time is: ${playerEntity.location.world.time}")
            arguments.size == 1 -> {
                val newTime = java.lang.Long.parseLong(arguments[0])
                playerEntity.location.world.time = newTime
                emitter.sendMessage("#82FFDBSet time to  :$newTime")
            }
            else -> emitter.sendMessage("#82FFDBSyntax : /time [0-10000]")
        }
        return true
    }

}
