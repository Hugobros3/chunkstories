//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.net.packets.PacketWorldUser
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.client.ingame.IngameClientImplementation
import java.io.DataInputStream

class ClientPacketWorldUser(world: World) : PacketWorldUser(world) {
    override fun receive(dis: DataInputStream, player: Player?) {
        super.receive(dis, player)
        val instance = world.gameInstance
        if (instance is Client) {
            val ingame = instance.ingame ?: return
            (ingame as IngameClientImplementation).loadingAgent.handleServerResponse(this)
        }
    }
}