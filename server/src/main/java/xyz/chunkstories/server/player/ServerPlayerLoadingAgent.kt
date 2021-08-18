package xyz.chunkstories.server.player

import xyz.chunkstories.net.packets.PacketWorldUser
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.api.world.region.Region
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ServerPlayerLoadingAgent (val player: ServerPlayer, val world: World) {
    val user = object : WorldUser {}

    private val regions = mutableSetOf<Region>()
    private val chunks = mutableSetOf<ChunkHolder>()
    private val lock = ReentrantLock()

    fun destroy() {
        lock.withLock {
            regions.removeAll {
                it.unregisterUser(user)
                true
            }

            chunks.removeAll {
                it.unregisterUser(user)
                true
            }
        }
    }

    fun handleClientRequest(packet: PacketWorldUser) {
        lock.withLock {
            when(packet.tag) {
                PacketWorldUser.Tag.REGISTER_CHUNK -> {
                    val chunkHolder = world.chunksManager.acquireChunkHolder(user, packet.x, packet.y, packet.z)!!
                    chunkHolder.registerUser(user)
                    chunks.add(chunkHolder)
                }
                PacketWorldUser.Tag.UNREGISTER_CHUNK -> {
                    val chunkHolder = chunks.find { it.chunkX == packet.x && it.chunkY == packet.y && it.chunkZ == packet.z } ?: throw Exception("hey you didn't register for that")
                    chunkHolder.unregisterUser(user)
                    chunks.remove(chunkHolder)
                }
                PacketWorldUser.Tag.REGISTER_SUMMARY -> TODO()
                PacketWorldUser.Tag.UNREGISTER_SUMMARY -> TODO()
            }
        }
    }
}