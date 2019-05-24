//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

class ModerationCommands(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("kick", this)
        server.pluginManager.registerCommand("ban", this)
        server.pluginManager.registerCommand("unban", this)
        server.pluginManager.registerCommand("banip", this)
        server.pluginManager.registerCommand("unbanip", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "kick" && emitter.hasPermission("server.admin.kick")) {
            if (arguments.size >= 1) {
                val clientByName = server.getPlayerByName(arguments[0])
                var kickReason = "Please refrain from further portrayal of such imbecile attitudes"
                // Recreate the argument
                if (arguments.size >= 2) {
                    kickReason = ""
                    for (i in 1 until arguments.size)
                        kickReason += arguments[i] + if (i < arguments.size - 1) " " else ""
                }

                if (clientByName != null) {
                    (clientByName as? RemotePlayer)?.disconnect("Kicked from server. \n$kickReason")
                    emitter.sendMessage("Kicked $clientByName for $kickReason")
                } else {
                    emitter.sendMessage("User '$clientByName' not found.")
                }
                return true
            } else {
                emitter.sendMessage("Syntax: /kick <playerName> [reason]")
                return true
            }
        }
        // TODO ban/unban commands
        return false
    }

}
