//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.vanillasockets

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.exceptions.net.IllegalPacketException
import xyz.chunkstories.api.exceptions.net.UnknowPacketException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.PacketDefinition.PacketGenre
import xyz.chunkstories.api.net.PacketWorldStreaming
import xyz.chunkstories.api.net.RemoteServer
import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.net.*
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.LogicalPacketDatagram
import xyz.chunkstories.net.PacketDefinition
import xyz.chunkstories.net.vanillasockets.SendQueue
import xyz.chunkstories.net.vanillasockets.StreamGobbler

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/** A clientside connection to a server using the TCP protocol.  */
open class TCPServerConnection(connectionSequence: ClientConnectionSequence) : ServerConnection(connectionSequence) {
    private val client: ClientImplementation = connectionSequence.client
    final override val encoderDecoder: ClientPacketsEncoderDecoder

    private var socket: Socket? = null

    private var connected = false
    private var disconnected = false

    private val connectOnce = AtomicBoolean(false)
    private val closeOnce = AtomicBoolean(false)

    private var sendQueue: SendQueue? = null

    // A representation of who we're talking to
    final override val remoteServer: RemoteServer

    override val isOpen: Boolean
        get() = connected && !disconnected

    init {
        remoteServer = RemoteServerImplementation(this)
        encoderDecoder = ClientPacketsEncoderDecoder(client, this)
    }

    override fun connect(): Boolean {
        if (!connectOnce.compareAndSet(false, true))
            return false

        try {
            socket = Socket(remoteAddress, port)

            val `in` = DataInputStream(socket!!.getInputStream())
            val streamGobbler = ClientGobbler(this, `in`)

            val out = DataOutputStream(socket!!.getOutputStream())
            sendQueue = SendQueue(this, out)

            connected = true

            streamGobbler.start()
            sendQueue!!.start()
            return true
        } catch (e: IOException) {
            return false
        }

    }

    internal inner class ClientGobbler(connection: Connection, `in`: DataInputStream) : StreamGobbler(connection, `in`)

    @Throws(IOException::class, PacketProcessingException::class, IllegalPacketException::class)
    override fun handleDatagram(datagram: LogicalPacketDatagram) {
        val definition = datagram.packetDefinition as PacketDefinition// (PacketDefinitionImpl)
        // getEncoderDecoder().getContentTranslator().getPacketForId(datagram.packetTypeId);
        if (definition.genre == PacketGenre.GENERAL_PURPOSE) {
            val packet = definition.createNew(true, null)
            packet!!.receive(remoteServer, datagram.data, encoderDecoder)
            datagram.dispose()

        } else if (definition.genre == PacketGenre.SYSTEM) {
            val packet = definition.createNew(true, null)
            packet!!.receive(remoteServer, datagram.data, encoderDecoder)
            if (packet is PacketText) {
                handleSystemRequest(packet.text)
            }
            datagram.dispose()

        } else if (definition.genre == PacketGenre.WORLD) {
            val world = encoderDecoder.world
            if (world == null) {
                Connection.Companion.logger.error("Received packet $definition but no world is up yet !")
            } else {
                world.queueDatagram(datagram)
            }

        } else if (definition.genre == PacketGenre.WORLD_STREAMING) {
            val world = encoderDecoder.world
            val packet = definition.createNew(true, world) as PacketWorldStreaming
            packet.receive(remoteServer, datagram.data, encoderDecoder)
            world.ioHandler().handlePacketWorldStreaming(packet)
            datagram.dispose()
        } else {
            throw RuntimeException("Unknown packet genre")
        }
    }

    override fun handleSystemRequest(msg: String): Boolean {
        if (msg.startsWith("chat/")) {
            val ingame = client.ingame
            ingame?.print(msg.substring(5))
        } else if (msg.startsWith("disconnect/")) {
            val serverKickReason = msg.replace("disconnect/", "")
            Connection.logger.info(serverKickReason)
            close("Disconnected by server: \n$serverKickReason")
        }

        return false
    }

    override fun pushPacket(packet: Packet) {
        try {
            sendQueue!!.queue(encoderDecoder.buildOutgoingPacket(packet))
        } catch (e: UnknowPacketException) {
            Connection.logger.error("Couldn't pushPacket()", e)
        } catch (e: IOException) {
            close("Failed to push packet")
        }

    }

    override fun flush() {
        if (sendQueue != null)
            sendQueue!!.flush()
    }

    override fun close(reason: String) {
        if (!closeOnce.compareAndSet(false, true))
            return

        onDisconnect(reason)

        try {
            if (socket != null)
                socket!!.close()
        } catch (e: IOException) {
            // Discard errors when disconnecting a connection
        }

        if (sendQueue != null)
            sendQueue!!.shutdown()

        disconnected = true
    }

}
