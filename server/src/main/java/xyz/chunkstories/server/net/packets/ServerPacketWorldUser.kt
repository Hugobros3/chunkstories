//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.net.packets

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.net.packets.PacketWorldUser
import xyz.chunkstories.server.player.ServerPlayer
import java.io.DataInputStream

class ServerPacketWorldUser(world: World?) : PacketWorldUser(world) {
    override fun receive(dis: DataInputStream, player: Player?) {
        super.receive(dis, player)
        if (player is ServerPlayer) {
            player.loadingAgent.handleClientRequest(this)
        }
    }
}