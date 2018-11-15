//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import java.io.IOException
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

import io.xol.chunkstories.api.content.OnlineContentTranslator
import io.xol.chunkstories.api.exceptions.PacketProcessingException
import io.xol.chunkstories.api.net.Packet
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre
import io.xol.chunkstories.api.net.PacketWorld
import io.xol.chunkstories.api.net.RemoteServer
import io.xol.chunkstories.api.sound.SoundManager
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.client.ingame.IngameClientRemoteHost
import io.xol.chunkstories.client.net.ServerConnection
import io.xol.chunkstories.content.translator.AbstractContentTranslator
import io.xol.chunkstories.net.LogicalPacketDatagram
import io.xol.chunkstories.net.PacketDefinitionImplementation
import io.xol.chunkstories.net.PacketsEncoderDecoder
import io.xol.chunkstories.world.io.IOTasks
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient

class WorldClientRemote @Throws(WorldLoadingException::class)
constructor(client: IngameClientRemoteHost, info: WorldInfo, translator: AbstractContentTranslator,
            val connection: ServerConnection) : WorldClientCommon(client, info, translator, null), WorldClientNetworkedRemote {
    private val packetsProcessor: PacketsEncoderDecoder

    override val ioHandler: IOTasksMultiplayerClient

    private val translator: OnlineContentTranslator

    override val soundManager: SoundManager
        get() = client.soundManager

    internal var incommingDatagrams: Deque<LogicalPacketDatagram> = ConcurrentLinkedDeque()

    init {
        this.packetsProcessor = connection.encoderDecoder

        this.translator = translator

        ioHandler = IOTasksMultiplayerClient(this)
        ioHandler.start()
    }

    override fun getRemoteServer(): RemoteServer {
        return connection.remoteServer
    }

    override fun destroy() {
        super.destroy()
        connection.close()
    }

    override fun tick() {
        // Specific MP stuff
        processIncommingPackets()

        super.tick()

        connection.flush()
    }

    // Accepts and processes synched packets
    fun processIncommingPackets() {
        try {
            entitiesLock.writeLock().lock()

            var packetsThisTick = 0

            val i = incommingDatagrams.iterator()
            while (i.hasNext()) {
                val datagram = i.next()

                try {
                    val definition = datagram.packetDefinition as PacketDefinitionImplementation // this.getContentTranslator().getPacketForId(datagram.packetTypeId);
                    val packet = definition.createNew(true, this)
                    if (definition.genre != PacketGenre.WORLD || packet !is PacketWorld) {
                        logger().error(definition.toString() + " isn't a PacketWorld")
                    } else {

                        // packetsProcessor.getSender() is equivalent to getRemoteServer() here
                        packet.process(packetsProcessor.interlocutor, datagram.data, packetsProcessor)
                    }
                } catch (e: IOException) {
                    logger().warn("Networking Exception while processing datagram: $e")
                } catch (e: PacketProcessingException) {
                    logger().warn("Networking Exception while processing datagram: $e")
                } catch (e: Exception) {
                    logger().warn("Exception while processing datagram: " + e.toString() + " " + e.message)
                }

                datagram.dispose()

                i.remove()
                packetsThisTick++
            }
        } finally {
            entitiesLock.writeLock().unlock()
        }
    }

    fun queueDatagram(datagram: LogicalPacketDatagram) {
        this.incommingDatagrams.add(datagram)
    }

    fun ioHandler(): IOTasksMultiplayerClient {
        return ioHandler
    }
}
