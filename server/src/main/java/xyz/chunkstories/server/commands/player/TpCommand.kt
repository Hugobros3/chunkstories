//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.player

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.commands.AbstractHostCommandHandler

class TpCommand(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("tp", this)
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

        val playerEntity = emitter.controlledEntity

        if (playerEntity == null) {
            emitter.sendMessage("You need to be controlling an entity")
            return true
        }

        var who: Player = emitter
        val what: Entity

        val to: Location

        when {
            arguments.size == 1 -> {
                val otherPlayer = host.getPlayer(arguments[0]) ?: throw Exception("#FF8966Player not found : " + arguments[0])
                val otherPlayerEntity = otherPlayer?.controlledEntity ?: throw Exception("#FF8966Player $otherPlayer is not controlling an entity currently...")

                to = otherPlayerEntity.location
                what = who.controlledEntity ?: throw Exception("#FF8966Player $who is not controlling an entity currently...")
            }
            arguments.size == 2 -> {
                who = host.getPlayer(arguments[0]) ?: throw Exception("#FF8966Player not found : " + arguments[0])
                what = who.controlledEntity ?: throw Exception("#FF8966Player $who is not controlling an entity currently...")

                val otherPlayer = host.getPlayer(arguments[1]) ?: throw Exception("#FF8966Player not found : " + arguments[1])
                val otherPlayerEntity = otherPlayer.controlledEntity ?: throw Exception("#FF8966Player $otherPlayer is not controlling an entity currently...")

                to = otherPlayerEntity.location
            }
            arguments.size == 3 -> {
                val x = Integer.parseInt(arguments[0])
                val y = Integer.parseInt(arguments[1])
                val z = Integer.parseInt(arguments[2])

                what = who.controlledEntity ?: throw Exception("#FF8966Player $who is not controlling an entity currently...")
                to = Location(what.location.world, x.toDouble(), y.toDouble(), z.toDouble())
            }
            arguments.size == 4 -> {
                who = host.getPlayer(arguments[0]) ?: throw Exception("#FF8966Player not found : " + arguments[0])

                val x = Integer.parseInt(arguments[1])
                val y = Integer.parseInt(arguments[2])
                val z = Integer.parseInt(arguments[3])

                what = who.controlledEntity ?: throw Exception("#FF8966Player $who is not controlling an entity currently...")
                to = Location(what.location.world, x.toDouble(), y.toDouble(), z.toDouble())
            }
            else -> {
                emitter.sendMessage("#FF8966Usage: /tp [who] (<x> <y> <z>)|(to)")
                return true
            }
        }

        emitter.sendMessage("#FF8966Teleported ${who.displayName} to : $to")
        what.location = to

        return true
    }

}
