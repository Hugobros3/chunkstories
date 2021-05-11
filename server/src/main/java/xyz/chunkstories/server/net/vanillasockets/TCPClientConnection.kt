//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net.vanillasockets

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.PacketsEncoderDecoder
import xyz.chunkstories.net.vanillasockets.SendQueue
import xyz.chunkstories.net.vanillasockets.StreamGobbler
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.net.ClientConnection
import xyz.chunkstories.server.net.ConnectionsManager
import xyz.chunkstories.world.WorldImplementation

class TCPClientConnection constructor(server: DedicatedServer, connectionsManager: ConnectionsManager, internal val socket: Socket) : ClientConnection(server, connectionsManager, socket.inetAddress.hostAddress, socket.port) {
    private val closeOnce = AtomicBoolean(false)
    private var disconnected = false

    private val streamGobbler: StreamGobbler
    private val sendQueue: SendQueue?

    override val isOpen: Boolean
        get() = !disconnected

    override val world: WorldImplementation?
        get() = server.world
    override val userConnection: UserConnection?
        get() = this
    override val encoderDecoder: PacketsEncoderDecoder
        get() = TODO("Not yet implemented")

    init {
        // We get exceptions early if this fails
        val socketInputStream = socket.getInputStream()
        val socketOutputStream = socket.getOutputStream()

        val inputDataStream = DataInputStream(BufferedInputStream(socketInputStream))
        val dataOutputStream = DataOutputStream(BufferedOutputStream(socketOutputStream))

        streamGobbler = ServerClientGobbler(this, inputDataStream)
        streamGobbler.start()

        sendQueue = SendQueue(this, dataOutputStream)
        sendQueue.start()
    }

    internal inner class ServerClientGobbler(connection: Connection, `in`: DataInputStream) : StreamGobbler(connection, `in`)

    override fun flush() {
        sendQueue!!.flush()
    }

    /*@Throws(IOException::class, PacketProcessingException::class, IllegalPacketException::class)
    override fun handleDatagram(datagram: LogicalPacketDatagram) {
        val definition = datagram.packetDefinition as PacketDefinition// getEncoderDecoder().getContentTranslator().getPacketForId(datagram.packetTypeId);
        if (definition.genre == PacketGenre.GENERAL_PURPOSE) {
            val packet = definition.createNew(true, null)
            packet!!.receive(encoderDecoder.interlocutor, datagram.data, encoderDecoder)
            datagram.dispose()

        } else if (definition.genre == PacketGenre.SYSTEM) {
            val packet = definition.createNew(true, null)
            packet!!.receive(encoderDecoder.interlocutor, datagram.data, encoderDecoder)
            if (packet is PacketText) {
                handleSystemRequest(packet.text)
            }
            datagram.dispose()

        } else if (definition.genre == PacketGenre.WORLD) {
            // Queue packets for a specific world
            if (player != null) {
                val world = player!!.controlledEntity?.world as? WorldServer
                if(world == null)
                    return
                world.queueDatagram(datagram, player!!)
            }
        } else if (definition.genre == PacketGenre.WORLD_STREAMING) {
            // Server doesn't expect world streaming updates from the client
            // it does, however, listen to world_user_requests packets to keep
            // track of the client's world data
            val world = encoderDecoder.world
            val packet = definition.createNew(false, world) as PacketWorldStreaming
            packet.receive(encoderDecoder.interlocutor, datagram.data, encoderDecoder)
            datagram.dispose()
        } else {
            throw RuntimeException("whut")
        }
    }*/

    override fun pushPacket(packet: Packet) {
        try {
            sendQueue!!.queue(encoderDecoder.buildOutgoingPacket(packet))
        }  catch (e: IOException) {
            close("IOException " + e.message)
        }
    }

    override fun close(reason: String) {
        if (!closeOnce.compareAndSet(false, true))
            return

        sendQueue?.shutdown()

        try {
            socket.close()
        } catch (e: IOException) {
            // Discard errors when disconnecting a connection
        }

        disconnected = true
        super.close(reason)
    }
}
