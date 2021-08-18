//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

class ModerationCommands(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("kick", this)
        host.pluginManager.registerCommand("ban", this)
        host.pluginManager.registerCommand("unban", this)
        host.pluginManager.registerCommand("banip", this)
        host.pluginManager.registerCommand("unbanip", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        when {
            command.name == "kick" && emitter.hasPermission("server.admin.kick") ->
                if (arguments.isNotEmpty()) {
                    val clientName = arguments[0]
                    val targetPlayer = host.getPlayer(clientName) ?: run {
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
                    val targetPlayer = host.getPlayer(clientName)

                    (targetPlayer as? RemotePlayer)?.disconnect("Banned.")
                    emitter.sendMessage("Banning $targetPlayer")
                    (host as DedicatedServer).userPrivileges.bannedUsers.add(clientName)
                }
            command.name == "unban" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val clientName = arguments[0]
                    val targetPlayer = host.getPlayer(clientName)

                    (targetPlayer as? RemotePlayer)?.disconnect("Banned.")
                    emitter.sendMessage("Unbanning $targetPlayer")
                    (host as DedicatedServer).userPrivileges.bannedUsers.remove(clientName)
                }
            command.name == "banip" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val ip = arguments[0]

                    emitter.sendMessage("Banning IP: $ip")
                    (host as DedicatedServer).userPrivileges.bannedIps.add(ip)
                }
            command.name == "unbanip" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val ip = arguments[0]

                    emitter.sendMessage("Unbanning IP: $ip")
                    (host as DedicatedServer).userPrivileges.bannedIps.remove(ip)
                }
            else -> return false
        }
        return true
    }

}
