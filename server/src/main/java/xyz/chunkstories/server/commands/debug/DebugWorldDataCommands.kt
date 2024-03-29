//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.server.commands.AbstractHostCommandHandler
import xyz.chunkstories.world.WorldImplementation

class DebugWorldDataCommands(serverConsole: Host) : AbstractHostCommandHandler(serverConsole) {

    init {
        host.pluginManager.registerCommand("chunk", this)
        host.pluginManager.registerCommand("region", this)
        host.pluginManager.registerCommand("heightmap", this)
        host.pluginManager.registerCommand("heightmaps", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "chunk" && emitter.hasPermission("server.debug")) {
            val player = emitter as Player
            val loc = player.state.location ?: kotlin.run {
                return true
            }
            TODO("get chunk intrinsic thing")
            emitter.sendMessage("#00FFD0" + loc.world)
            return true
        } else if (command.name == "region" && emitter.hasPermission("server.debug")) {
            val player = emitter as Player

            val chunk = TODO() //player.controlledEntity!!.traitLocation.chunk

            /*if (chunk != null)
                emitter.sendMessage("#00FFD0" + chunk.region)
            else
                emitter.sendMessage("#00FFD0" + "not within a loaded chunk, so no parent region could be found.")

            return true*/
        } else if (command.name == "heightmap" && emitter.hasPermission("server.debug")) {
            val sum: Heightmap?

            if (arguments.size == 2) {
                val x = Integer.parseInt(arguments[0])
                val z = Integer.parseInt(arguments[1])
                sum = host.world.heightmapsManager.getHeightmap(x, z)
            } else {
                val player = emitter as Player
                val playerEntity = player.entityIfIngame ?: throw Exception("Not currently controlling an entity !")
                sum = playerEntity.world.heightmapsManager.getHeightmapLocation(playerEntity.location)
            }

            emitter.sendMessage("#00FFD0" + sum!!)
            return true
        } else if (command.name == "heightmaps" && emitter.hasPermission("server.debug")) {
            dumpLoadedHeightmap(host.world as WorldImplementation, emitter)
        }
        return false
    }

    private fun dumpLoadedHeightmap(world: WorldImplementation, emitter: CommandEmitter) {
        emitter.sendMessage("#00FFD0" + "Dumping all region summaries...")
        for (sum in world.heightmapsManager.all()) {
            emitter.sendMessage("#00FFD0$sum")
        }
    }
}
