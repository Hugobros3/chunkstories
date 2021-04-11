//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.entity.traits.serializable.TraitFlyingMode
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

/** Regulates flying  */
class FlyCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("fly", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {

        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        if (!emitter.hasPermission("self.toggleFly")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }

        val entity = emitter.controlledEntity

        entity?.traits?.get(TraitFlyingMode::class)?.let { fm ->
            var state = fm.isAllowed
            state = !state
            emitter.sendMessage("Flying mode set to: $state")
            fm.isAllowed = state

            return true
        }
        emitter.sendMessage("This action doesn't apply to your current entity.")

        return true
    }

}
