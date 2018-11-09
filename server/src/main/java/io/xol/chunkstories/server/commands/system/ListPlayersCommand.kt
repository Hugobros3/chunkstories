//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands.system

import io.xol.chunkstories.api.plugin.commands.Command
import io.xol.chunkstories.api.plugin.commands.CommandEmitter
import io.xol.chunkstories.api.server.Server
import io.xol.chunkstories.server.commands.ServerCommandBasic

class ListPlayersCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("list").handler = this
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "list") {

            val players = server.connectedPlayers
            val list = players.map { it.name }.toString()

            emitter.sendMessage("#00FFD0${players.size} players connected : $list")
            return true
        }

        return false
    }

}
