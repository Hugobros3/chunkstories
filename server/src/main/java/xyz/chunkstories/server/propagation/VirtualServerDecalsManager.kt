//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.propagation

import xyz.chunkstories.world.WorldMasterImplementation
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.server.player.ServerPlayer
import org.joml.Vector3dc
import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.net.packets.PacketDecal

class VirtualServerDecalsManager(private val world: WorldMasterImplementation, server: DedicatedServer?) : DecalsManager {
    inner class ServerPlayerVirtualDecalsManager(var serverPlayer: ServerPlayer) : DecalsManager {
        override fun add(position: Vector3dc, orientation: Vector3dc, size: Vector3dc, decalName: String) {
            this@VirtualServerDecalsManager.add(position, orientation, size, decalName)
        }
    }

    override fun add(position: Vector3dc, orientation: Vector3dc, size: Vector3dc, decalName: String) {
        for (player in world.players) {
            if (player !is RemotePlayer) continue
            val packet = PacketDecal(world, decalName, position, orientation, size)
            player.pushPacket(packet)
        }
    }
}