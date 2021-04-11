//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.server.DedicatedServer

class StopServerCommand(val host: DedicatedServer) : CommandHandler {

    init {
        this.host.pluginManager.registerCommand("stop", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name.startsWith("stop") && emitter.hasPermission("server.stop")) {
            emitter.sendMessage("Stopping server.")
            host.requestShutdown()
            return true
        }
        return false
    }

}
