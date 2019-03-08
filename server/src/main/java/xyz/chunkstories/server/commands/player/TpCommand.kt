//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic

class TpCommand(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("tp", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {

        if (!emitter.hasPermission("server.tp")) {
            emitter.sendMessage("You don't have the permission.")
            return true
        }

        if (emitter !is Player) {
            emitter.sendMessage("You need to be a player to use this command.")
            return true
        }

        var who: Player? = emitter

        var to: Location? = null

        if (arguments.size == 1) {
            val otherPlayer = server.getPlayerByName(arguments[0])
            if (otherPlayer != null)
                to = otherPlayer.location
            else
                emitter.sendMessage("#FF8966Player not found : " + arguments[0])
        } else if (arguments.size == 2) {
            who = server.getPlayerByName(arguments[0])
            if (who == null)
                emitter.sendMessage("#FF8966Player not found : " + arguments[0])

            val otherPlayer = server.getPlayerByName(arguments[1])
            if (otherPlayer != null)
                to = otherPlayer.location
            else
                emitter.sendMessage("#FF8966Player not found : " + arguments[1])
        } else if (arguments.size == 3) {
            val x = Integer.parseInt(arguments[0])
            val y = Integer.parseInt(arguments[1])
            val z = Integer.parseInt(arguments[2])

            to = Location(who!!.location.world, x.toDouble(), y.toDouble(), z.toDouble())
        } else if (arguments.size == 4) {
            who = server.getPlayerByName(arguments[0])
            if (who == null)
                emitter.sendMessage("#FF8966Player not found : " + arguments[0])

            val x = Integer.parseInt(arguments[1])
            val y = Integer.parseInt(arguments[2])
            val z = Integer.parseInt(arguments[3])

            to = Location(who!!.location.world, x.toDouble(), y.toDouble(), z.toDouble())
        }

        if (who != null && to != null) {
            emitter.sendMessage("#FF8966Teleported to : $to")
            who.location = to
            return true
        }

        emitter.sendMessage("#FF8966Usage: /tp [who] (<x> <y> <z>)|(to)")

        return true
    }

}
