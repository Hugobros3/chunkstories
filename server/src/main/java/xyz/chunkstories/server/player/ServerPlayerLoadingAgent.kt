package xyz.chunkstories.server.player

import xyz.chunkstories.api.net.packets.PacketWorldUser
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.region.Region
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ServerPlayerLoadingAgent (val player: ServerPlayer, val world: World) {

    private val regions = mutableSetOf<Region>()
    private val chunks = mutableSetOf<ChunkHolder>()
    private val lock = ReentrantLock()

    fun destroy() {
        lock.withLock {
            regions.removeAll {
                it.unregisterUser(player)
                true
            }

            chunks.removeAll {
                it.unregisterUser(player)
                true
            }
        }
    }

    fun handleClientRequest(packet: PacketWorldUser) {
        lock.withLock {
            when(packet.type) {
                null -> throw Exception("PacketWorldUser type can't be null")

                PacketWorldUser.Type.REGISTER_CHUNK -> {
                    val chunkHolder = world.chunksManager.acquireChunkHolder(player, packet.x, packet.y, packet.z)!!
                    chunkHolder.registerUser(player)
                    chunks.add(chunkHolder)
                }
                PacketWorldUser.Type.UNREGISTER_CHUNK -> {
                    val chunkHolder = chunks.find { it.chunkX == packet.x && it.chunkY == packet.y && it.chunkZ == packet.z } ?: throw Exception("hey you didn't register for that")
                    chunkHolder.unregisterUser(player)
                    chunks.remove(chunkHolder)
                }
                PacketWorldUser.Type.REGISTER_SUMMARY -> TODO()
                PacketWorldUser.Type.UNREGISTER_SUMMARY -> TODO()
            }
        }
    }
}