//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

/** Heals  */
class HealthCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("health", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {

        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        if (!emitter.hasPermission("self.sethealth")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }

        if (arguments.size < 1 || !isNumeric(arguments[0])) {
            emitter.sendMessage("Syntax: /health <hp>")
            return true
        }

        val health = java.lang.Float.parseFloat(arguments[0])

        val entity = emitter.controlledEntity

        entity?.traits?.get(TraitHealth::class)?.let { fm ->
            fm.health = health
            emitter.sendMessage("Health set to: " + health + "/" + fm.maxHealth)

            return true
        }
        emitter.sendMessage("This action doesn't apply to your current entity.")

        return true
    }

    companion object {

        // Lazy, why does Java standard lib doesn't have a clean way to do this tho
        // http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
        fun isNumeric(str: String): Boolean {
            for (c in str.toCharArray()) {
                if (!Character.isDigit(c))
                    return false
            }
            return true
        }
    }

}
