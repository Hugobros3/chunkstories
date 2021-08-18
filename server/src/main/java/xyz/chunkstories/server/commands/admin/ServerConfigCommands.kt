//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

class ServerConfigCommands(val host: DedicatedServer) : CommandHandler {

    init {
        this.host.pluginManager.registerCommand("reloadConfig", this)
        this.host.pluginManager.registerCommand("op", this)
        this.host.pluginManager.registerCommand("deop", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        when {
            command.name == "reloadConfig" && emitter.hasPermission("server.reloadConfig") -> {
                host.reloadConfig()
                emitter.sendMessage("Config reloaded.")
            }
            command.name == "op" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val targetPlayerName = arguments[0]

                    emitter.sendMessage("Made $targetPlayerName a server administrator.")
                    host.userPrivileges.admins.add(targetPlayerName)
                }

            command.name == "deop" && emitter.hasPermission("server.admin.ban") ->
                if (arguments.isNotEmpty()) {
                    val targetPlayerName = arguments[0]

                    emitter.sendMessage("Made $targetPlayerName not an admin anymore")
                    host.userPrivileges.admins.remove(targetPlayerName)
                }
            else -> return false
        }
        return true
    }

}
