//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import java.io.File
import java.io.IOException
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.exceptions.PacketProcessingException
import io.xol.chunkstories.api.net.Packet
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre
import io.xol.chunkstories.api.net.PacketWorld
import io.xol.chunkstories.api.net.packets.PacketTime
import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.util.IterableIterator
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.api.world.WorldNetworked
import io.xol.chunkstories.net.LogicalPacketDatagram
import io.xol.chunkstories.net.PacketDefinitionImplementation
import io.xol.chunkstories.server.DedicatedServer
import io.xol.chunkstories.server.player.ServerPlayer
import io.xol.chunkstories.server.propagation.VirtualServerDecalsManager
import io.xol.chunkstories.server.propagation.VirtualServerParticlesManager
import io.xol.chunkstories.sound.VirtualSoundManager
import io.xol.chunkstories.world.io.IOTasks

class WorldServer @Throws(WorldLoadingException::class)
constructor(val server: DedicatedServer, worldInfo: WorldInfo, folder: File) : WorldImplementation(server, worldInfo, null, folder), WorldMaster, WorldNetworked {
    override val ioHandler: IOTasks

    //private final AbstractContentTranslator translator;

    override val soundManager: VirtualSoundManager
    override val particlesManager: VirtualServerParticlesManager
    override val decalsManager: VirtualServerDecalsManager

    private val packetsQueue = ConcurrentLinkedDeque<PendingPlayerDatagram>()

    override
            /** We cast the super property because of callbacks in the superconstructor expect this to be set before we have a chance to in this constructor  */
    val gameContext: DedicatedServer
        get() = super.gameContext as DedicatedServer

    override//TODO make sure they are in that world
    val players: Set<Player>
        get() = server.connectedPlayers

    init {

        //this.translator = (AbstractContentTranslator) super.getContentTranslator();

        this.soundManager = VirtualSoundManager(this)
        this.particlesManager = VirtualServerParticlesManager(this, server)
        this.decalsManager = VirtualServerDecalsManager(this, server)

        ioHandler = IOTasks(this)
        ioHandler.start()
    }

    override fun tick() {
        processIncommingPackets()

        super.tick()

        // Update client tracking
        for (player in players) {

            if (player.hasSpawned()) {
                // Update whatever he sees
                (player as ServerPlayer).updateTrackedEntities()
            }

            // Update time & weather
            val packetTime = PacketTime(this)
            packetTime.time = this.time
            packetTime.overcastFactor = this.weather
            player.pushPacket(packetTime)
        }

        soundManager.update()

        // TODO this should work per-world
        this.server.handler.flushAll()
    }

    internal inner class PendingPlayerDatagram(var datagram: LogicalPacketDatagram, var player: ServerPlayer)

    fun processIncommingPackets() {
        try {
            entitiesLock.writeLock().lock()

            val iterator = packetsQueue.iterator()
            while (iterator.hasNext()) {
                val incomming = iterator.next()
                iterator.remove()

                val player = incomming.player
                val datagram = incomming.datagram

                try {
                    val definition = datagram.packetDefinition as PacketDefinitionImplementation // this.getContentTranslator().getPacketForId(datagram.packetTypeId);
                    val packet = definition.createNew(false, this)

                    if (definition.genre != PacketGenre.WORLD || packet !is PacketWorld) {
                        logger().error(definition.toString() + " isn't a PacketWorld")
                    } else {

                        // packetsProcessor.getSender() is equivalent to player here
                        packet.process(player, datagram.data,
                                player.playerConnection.encoderDecoder)
                    }
                } catch (e: IOException) {
                    logger().warn("Networking Exception while processing datagram: " + e.message)
                } catch (e: PacketProcessingException) {
                    logger().warn("Networking Exception while processing datagram: " + e.message)
                } catch (e: Exception) {
                    logger().warn("Exception while processing datagram: " + e.message)
                }

                datagram.dispose()
            }
        } finally {
            entitiesLock.writeLock().unlock()
        }
    }

    fun queueDatagram(datagram: LogicalPacketDatagram, player: ServerPlayer) {
        packetsQueue.addLast(PendingPlayerDatagram(datagram, player))
    }

    override fun getPlayerByName(playerName: String): Player? {
        // Does the server have this player ?
        val player = server.getPlayerByName(playerName) ?: return null

        // We don't want players from other worlds
        return if (player.world != this) null else player

    }
}
