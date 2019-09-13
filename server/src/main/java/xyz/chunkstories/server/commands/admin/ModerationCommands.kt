//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.DedicatedServer
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
        when {
            command.name == "kick" && emitter.hasPermission("server.admin.kick") ->
                if (arguments.isNotEmpty()) {
                    val clientName = arguments[0]
                    val targetPlayer = server.getPlayerByName(clientName) ?: run {
                        emitter.sendMessage("User '$clientName' not found.")
                        return true
                    }
                    var kickReason = "Please refrain from further portrayal of such imbecile attitudes"

                    // Recreate the argument
                    if (arguments.size >= 2) {
                        kickReason = ""
                        for (i in 1 until arguments.size)
                            kickReason += arguments[i] + if (i < arguments.size - 1) " " else ""
                    }

                    (targetPlayer as? RemotePlayer)?.disconnect("Kicked from server. \n$kickReason")
                    emitter.sendMessage("Kicked $targetPlayer for $kickReason")

                } else {
                    emitter.sendMessage("Syntax: /kick <playerName> [reason]")
                }
            command.name == "ban" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val clientName = arguments[0]
                    val targetPlayer = server.getPlayerByName(clientName)

                    (targetPlayer as? RemotePlayer)?.disconnect("Banned.")
                    emitter.sendMessage("Banning $targetPlayer")
                    (server as DedicatedServer).userPrivileges.bannedUsers.add(clientName)
                }
            command.name == "unban" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val clientName = arguments[0]
                    val targetPlayer = server.getPlayerByName(clientName)

                    (targetPlayer as? RemotePlayer)?.disconnect("Banned.")
                    emitter.sendMessage("Unbanning $targetPlayer")
                    (server as DedicatedServer).userPrivileges.bannedUsers.remove(clientName)
                }
            command.name == "banip" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val ip = arguments[0]

                    emitter.sendMessage("Banning IP: $ip")
                    (server as DedicatedServer).userPrivileges.bannedIps.add(ip)
                }
            command.name == "unbanip" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val ip = arguments[0]

                    emitter.sendMessage("Unbanning IP: $ip")
                    (server as DedicatedServer).userPrivileges.bannedIps.remove(ip)
                }
            else -> return false
        }
        return true
    }

}
