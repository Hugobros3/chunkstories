//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import xyz.chunkstories.world.WorldImplementation

class DebugIOCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("io", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "io" && emitter.hasPermission("server.debug")) {
            emitter.sendMessage("#00FFD0" + (host.world as WorldImplementation).ioHandler)
            (host.world as WorldImplementation).ioHandler.dumpIOTaks()
            return true
        }
        return false
    }

}
