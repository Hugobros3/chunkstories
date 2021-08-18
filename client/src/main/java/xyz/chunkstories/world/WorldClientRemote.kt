//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world
/*
import java.io.IOException
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.client.ingame.IngameClientRemoteHost
import xyz.chunkstories.client.net.ServerConnection
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.net.LogicalPacketDatagram
import xyz.chunkstories.net.PacketDefinition
import xyz.chunkstories.net.PacketsEncoderDecoder

class WorldClientRemote @Throws(WorldLoadingException::class)
constructor(client: IngameClientRemoteHost, info: WorldInfo, translator: AbstractContentTranslator, val connection: ServerConnection) : WorldClientCommon(client, info, translator, null), WorldClientNetworkedRemote {
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
        //connection.close()
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
                    val definition = datagram.packetDefinition as PacketDefinition // this.getContentTranslator().getPacketForId(datagram.packetTypeId);
                    val packet = definition.createNew(true, this)
                    if (definition.genre != PacketGenre.WORLD || packet !is PacketWorld) {
                        logger.error("$definition isn't a PacketWorld")
                    } else {

                        // packetsProcessor.getSender() is equivalent to getRemoteServer() here
                        packet.receive(packetsProcessor.interlocutor, datagram.data, packetsProcessor)
                    }
                } catch (e: IOException) {
                    logger.warn("Networking Exception while processing datagram: $e")
                } catch (e: PacketProcessingException) {
                    logger.warn("Networking Exception while processing datagram: $e")
                } catch (e: Exception) {
                    logger.warn("Exception while processing datagram: " + e.toString() + " " + e.message)
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
*/