//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

class EntitiesDebugCommands(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("entities", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "entities" && emitter.hasPermission("server.debug")) {
            val entities = server.world.allLoadedEntities
            while (entities.hasNext()) {
                val entity = entities.next()
                emitter.sendMessage("#FFDD00$entity")
            }
            return true
        }
        return false
    }

}
