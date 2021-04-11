//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.admin

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldMasterImplementation

class SaveCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("save", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "save" && emitter.hasPermission("server.admin.forcesave")) {
            emitter.sendMessage("#00FFD0Saving the world...")
            (host.world as WorldMasterImplementation).saveEverything()
            return true
        }
        return false
    }

}
