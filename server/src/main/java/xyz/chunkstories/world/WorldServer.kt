//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.net.packets.PacketTime
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.net.LogicalPacketDatagram
import xyz.chunkstories.net.PacketDefinition
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.server.propagation.VirtualServerDecalsManager
import xyz.chunkstories.server.propagation.VirtualServerParticlesManager
import xyz.chunkstories.sound.VirtualSoundManager
import xyz.chunkstories.world.io.IOTasks
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedDeque

/*class WorldServer @Throws(WorldLoadingException::class)
constructor(val server: DedicatedServer, worldInfo: WorldInfo, folder: File) : WorldImplementation(server, worldInfo, null, folder), WorldMaster, WorldCommon, WorldNetworked {
    override val ioHandler: IOTasks

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
        get() = server.players

    init {
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
            if(player !is ServerPlayer)
                continue

            // Update whatever he sees
            player.updateTrackedEntities()

            // Update time & weather
            val packetTime = PacketTime(this)
            packetTime.tod = this.sunCycle
            packetTime.overcastFactor = this.weather
            player.pushPacket(packetTime)
        }

        soundManager.update()

        // TODO this should work per-world
        this.server.connectionsManager.flushAll()
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
                    val definition = datagram.packetDefinition as PacketDefinition // this.getContentTranslator().getPacketForId(datagram.packetTypeId);
                    val packet = definition.createNew(false, this)

                    if (definition.genre != PacketGenre.WORLD || packet !is PacketWorld) {
                        logger.error("$definition isn't a PacketWorld")
                    } else {

                        // packetsProcessor.getSender() is equivalent to player here
                        packet.receive(player, datagram.data,
                                player.playerConnection.encoderDecoder)
                    }
                } catch (e: IOException) {
                    logger.warn("Networking Exception while processing datagram: " + e.message)
                } catch (e: PacketProcessingException) {
                    logger.warn("Networking Exception while processing datagram: " + e.message)
                } catch (e: Exception) {
                    logger.warn("Exception while processing datagram: " + e.message)
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

    override
}
*/