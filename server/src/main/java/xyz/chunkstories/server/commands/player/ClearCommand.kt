//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

/** Removes all items from inventory  */
class ClearCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("clear", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (!emitter.hasPermission("self.clearinventory")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }
        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        val entity = emitter.controlledEntity
        entity?.traits?.get(TraitInventory::class)?.let { ei ->
            emitter.sendMessage("#FF969BRemoving " + ei.size() + " items from your inventory.")
            ei.clear()
        }

        return true
    }

}
