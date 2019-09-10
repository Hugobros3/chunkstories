//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.entity.traits.serializable.TraitCreativeMode
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

/** Handles creativity  */
class CreativeCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("creative", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {

        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        if (!emitter.hasPermission("self.toggleCreative")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }

        val entity = emitter.controlledEntity
        entity?.traits?.get(TraitCreativeMode::class.java)?.let { fm ->
            var state = fm.enabled
            state = !state
            emitter.sendMessage("Creative mode set to: $state")
            fm.enabled = state

            return false
        }
        emitter.sendMessage("This action doesn't apply to your current entity.")

        return true
    }

}
