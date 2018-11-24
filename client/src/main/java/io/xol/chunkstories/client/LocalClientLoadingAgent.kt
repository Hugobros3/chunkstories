//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client

import java.util.HashSet
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import com.carrotsearch.hppc.IntHashSet

import io.xol.chunkstories.api.client.Client
import io.xol.chunkstories.api.client.LocalPlayer
import io.xol.chunkstories.api.entity.Entity
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException
import io.xol.chunkstories.api.math.LoopingMathHelper
import io.xol.chunkstories.api.math.Math2
import io.xol.chunkstories.api.net.packets.PacketWorldUser
import io.xol.chunkstories.api.net.packets.PacketWorldUser.Type
import io.xol.chunkstories.api.world.WorldClient
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.api.world.WorldSize
import io.xol.chunkstories.api.world.chunk.ChunkHolder
import io.xol.chunkstories.api.world.heightmap.Heightmap
import io.xol.chunkstories.api.world.region.Region
import io.xol.chunkstories.world.WorldClientRemote

class LocalClientLoadingAgent(private val client: Client, private val player: LocalPlayer, private val world: WorldClient) {

    val fastChunksMask = IntHashSet()

    val usedChunks = HashSet<ChunkHolder>()
    private val usedRegionSummaries = HashSet<Heightmap>()

    private val lock = ReentrantLock()

    fun updateUsedWorldBits() {
        val controlledEntity = player.controlledEntity ?: return

        try {
            lock.lock()

            // Subscribe to nearby wanted chunks
            val cameraChunkX = Math2.floor(controlledEntity.location.x() / 32)
            val cameraChunkY = Math2.floor(controlledEntity.location.y() / 32)
            val cameraChunkZ = Math2.floor(controlledEntity.location.z() / 32)

            val chunksViewDistance = world.client.configuration.getIntValue(InternalClientOptions.viewDistance) / 32
            val chunksViewDistanceHeight = 4

            for (chunkX in cameraChunkX - chunksViewDistance - 1..cameraChunkX + chunksViewDistance + 1) {
                for (chunkZ in cameraChunkZ - chunksViewDistance - 1..cameraChunkZ + chunksViewDistance + 1)
                    for (chunkY in cameraChunkY - chunksViewDistanceHeight - 1..cameraChunkY + chunksViewDistanceHeight + 1) {
                        if (chunkY < 0 || chunkY >= world.worldInfo.size.heightInChunks)
                            continue

                        val worldInfo = world.worldInfo
                        val size = worldInfo.size

                        val filteredChunkX = chunkX and size.maskForChunksCoordinates
                        val filteredChunkY = Math2.clampi(chunkY, 0, size.sizeInChunks - 1)
                        val filteredChunkZ = chunkZ and size.maskForChunksCoordinates

                        val summed = (((filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates) or filteredChunkY) shl size.bitlengthOfHorizontalChunksCoordinates) or filteredChunkZ
                        //val summed2 = filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates or filteredChunkY shl size.bitlengthOfHorizontalChunksCoordinates or filteredChunkZ
                        //if(summed != summed2)
                        //    println("fuck")

                        //println(" d ${size.bitlengthOfVerticalChunksCoordinates} o ${size.bitlengthOfHorizontalChunksCoordinates}")

                        if (fastChunksMask.contains(summed)) {
                            //val chunkHolder = usedChunks.find { it.chunkX == filteredChunkX && it.chunkY == filteredChunkY && it.chunkZ == filteredChunkZ }
                            //println("$chunkHolder + ${chunkHolder?.region}")
                            continue
                        }

                        val holder = world.acquireChunkHolder(player, chunkX, chunkY, chunkZ)!!

                        if(holder.region.state is Region.State.Zombie)
                            throw Exception("WOW THIS IS NOT COOL :(((")

                        usedChunks.add(holder)
                        fastChunksMask.add(summed)

                        if (world is WorldClientRemote) {
                            world.connection.pushPacket(PacketWorldUser.registerChunkPacket(world, filteredChunkX, filteredChunkY, filteredChunkZ))
                        }
                    }
            }

            // Unsubscribe for far ones
            val i = usedChunks.iterator()
            while (i.hasNext()) {
                val holder = i.next()
                if (LoopingMathHelper.moduloDistance(holder.chunkX, cameraChunkX, world.sizeInChunks) > chunksViewDistance + 1
                        || LoopingMathHelper.moduloDistance(holder.chunkZ, cameraChunkZ, world.sizeInChunks) > chunksViewDistance + 1
                        || Math.abs(holder.chunkY - cameraChunkY) > chunksViewDistanceHeight + 1) {
                    val worldInfo = world.worldInfo
                    val size = worldInfo.size

                    val filteredChunkX = holder.chunkX and size.maskForChunksCoordinates
                    val filteredChunkY = Math2.clampi(holder.chunkY, 0, 31)
                    val filteredChunkZ = holder.chunkZ and size.maskForChunksCoordinates

                    val summed = (((filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates) or filteredChunkY) shl size.bitlengthOfHorizontalChunksCoordinates) or filteredChunkZ

                    fastChunksMask.remove(summed)

                    i.remove()
                    holder.unregisterUser(player)

                    if (world is WorldClientRemote) {
                        world.connection.pushPacket(PacketWorldUser.unregisterChunkPacket(world, filteredChunkX, filteredChunkY, filteredChunkZ))
                    }
                }
            }

            // We load the region summaries we fancy
            val summaryDistance = 32//(int) (world.getClient().getConfiguration().getIntOption("client.rendering.viewDistance") / 24);
            for (chunkX in cameraChunkX - summaryDistance until cameraChunkX + summaryDistance)
                for (chunkZ in cameraChunkZ - summaryDistance until cameraChunkZ + summaryDistance) {
                    if (chunkX % 8 == 0 && chunkZ % 8 == 0) {
                        val regionX = chunkX / 8
                        val regionZ = chunkZ / 8

                        // TODO bad to acquire each time!!!
                        val regionSummary = world.regionsSummariesHolder.acquireHeightmap(player, regionX,
                                regionZ)

                        if (usedRegionSummaries.add(regionSummary)) {

                            if (world is WorldClientRemote) {

                                world.connection.pushPacket(PacketWorldUser.registerSummary(world, regionX, regionZ))
                            }
                        }
                    }
                }

            val cameraRegionX = cameraChunkX / 8
            val cameraRegionZ = cameraChunkZ / 8

            val distInRegions = summaryDistance / 8
            val sizeInRegions = world.sizeInChunks / 8

            // And we unload the ones we no longer need
            val iterator = usedRegionSummaries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val regionX = entry.regionX
                val regionZ = entry.regionZ

                val dx = LoopingMathHelper.moduloDistance(cameraRegionX, regionX, sizeInRegions)
                val dz = LoopingMathHelper.moduloDistance(cameraRegionZ, regionZ, sizeInRegions)
                if (dx > distInRegions + 1 || dz > distInRegions + 1) {
                    entry.unregisterUser(player)
                    iterator.remove()

                    if (world is WorldClientRemote) {
                        world.connection.pushPacket(PacketWorldUser.unregisterSummary(world, regionX, regionZ))
                    }
                }
            }

        } finally {
            lock.unlock()
        }
    }

    @Throws(IllegalPacketException::class)
    fun handleServerResponse(packet: PacketWorldUser) {

        try {
            lock.lock()
            // The server refused to register us to this chunk. We gracefully accept.
            if (packet.type == Type.UNREGISTER_CHUNK) {
                val holder = world.getRegionChunkCoordinates(packet.x, packet.y, packet.z)!!
                        .getChunkHolder(packet.x, packet.y, packet.z) ?: return

                // Apparently we already figured we didn't need this anyway

                val worldInfo = world.worldInfo
                val size = worldInfo.size

                val filteredChunkX = holder.chunkX and size.maskForChunksCoordinates
                val filteredChunkY = Math2.clampi(holder.chunkY, 0, 31)
                val filteredChunkZ = holder.chunkZ and size.maskForChunksCoordinates

                val summed = filteredChunkX shl size.bitlengthOfVerticalChunksCoordinates or filteredChunkY shl size.bitlengthOfHorizontalChunksCoordinates or filteredChunkZ

                // We remove it from our list
                fastChunksMask.remove(summed)
                usedChunks.remove(holder)

                // And we unsub.
                holder.unregisterUser(player)

                // This is the same but for region summaries
            } else if (packet.type == Type.UNREGISTER_SUMMARY) {
                val regionSummary = world.regionsSummariesHolder.getHeightmap(packet.x, packet.z) ?: return

                usedRegionSummaries.remove(regionSummary)
                regionSummary.unregisterUser(player)

            } else
            // We only expect UNREGISTER packets from the server !
                throw IllegalPacketException(packet)
        } finally {
            lock.unlock()
        }
    }
}
