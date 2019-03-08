//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.debug

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.heightmap.Heightmap
import xyz.chunkstories.server.commands.ServerCommandBasic
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.heightmap.HeightmapImplementation

class DebugWorldDataCommands(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("chunk", this)
        server.pluginManager.registerCommand("region", this)
        server.pluginManager.registerCommand("heightmap", this)
        server.pluginManager.registerCommand("heightmaps", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>): Boolean {
        if (command.name == "chunk" && emitter.hasPermission("server.debug")) {
            val player = emitter as Player

            emitter.sendMessage("#00FFD0" + player.controlledEntity!!.traitLocation.chunk!!)
            return true
        } else if (command.name == "region" && emitter.hasPermission("server.debug")) {
            val player = emitter as Player

            val chunk = player.controlledEntity!!.traitLocation.chunk

            if (chunk != null)
                emitter.sendMessage("#00FFD0" + chunk.region)
            else
                emitter.sendMessage("#00FFD0" + "not within a loaded chunk, so no parent region could be found.")

            return true
        } else if (command.name == "heightmap" && emitter.hasPermission("server.debug")) {
            val sum: Heightmap?

            if (arguments.size == 2) {
                val x = Integer.parseInt(arguments[0])
                val z = Integer.parseInt(arguments[1])
                sum = server.world.regionsSummariesHolder.getHeightmap(x, z)
            } else {
                val player = emitter as Player
                val playerEntity = player.controlledEntity ?: throw Exception("Not currently controlling an entity !")
                sum = playerEntity.world.regionsSummariesHolder.getHeightmapLocation(playerEntity.location)
            }

            emitter.sendMessage("#00FFD0" + sum!!)
            return true
        } else if (command.name == "heightmaps" && emitter.hasPermission("server.debug")) {
            dumpLoadedHeightmap(server.world as WorldImplementation, emitter)
        }
        return false
    }

    private fun dumpLoadedHeightmap(world: WorldImplementation, emitter: CommandEmitter) {
        emitter.sendMessage("#00FFD0" + "Dumping all region summaries...")
        for (sum in world.regionsSummariesHolder.all()) {
            emitter.sendMessage("#00FFD0$sum")
        }
    }
}
