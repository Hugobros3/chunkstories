//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.commands.ServerCommandBasic

class StopServerCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("stop", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name.startsWith("stop") && emitter.hasPermission("server.stop")) {
            if (server is DedicatedServer) {
                emitter.sendMessage("Stopping server.")
                server.stop()
                return true
            } else {
                //TODO instead of being a smartass just register the command in DedicatedServer.java ?
                emitter.sendMessage("This isn't a dedicated server.")
                return true
            }
        }
        return false
    }

}
