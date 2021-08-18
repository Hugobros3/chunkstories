//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.vanillasockets

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.exceptions.net.IllegalPacketException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.api.world.WorldSub
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.net.*
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.LogicalPacketDatagram
import xyz.chunkstories.net.PacketDefinition
import xyz.chunkstories.net.vanillasockets.SendQueue
import xyz.chunkstories.net.vanillasockets.StreamGobbler
import xyz.chunkstories.world.WorldImplementation

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/** A clientside connection to a server using the TCP protocol.  */
open class TCPServerConnection(client: ClientImplementation, connectionSequence: ClientConnectionSequence) : ServerConnection(client, connectionSequence) {
    private val client: ClientImplementation = connectionSequence.client
    final override val encoderDecoder: ClientPacketsEncoderDecoder

    private var socket: Socket? = null

    private var connected = false
    private var disconnected = false

    private val connectOnce = AtomicBoolean(false)
    private val closeOnce = AtomicBoolean(false)

    private var sendQueue: SendQueue? = null

    override val isOpen: Boolean
        get() = connected && !disconnected

    override val remoteServer = RemoteServerImplementation(this)

    override val world: WorldImplementation?
        get() = client.ingame?.world

    override val userConnection: UserConnection?
        get() = null

    init {
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

    override fun handleSystemRequest(message: String): Boolean {
        if (message.startsWith("chat/")) {
            val ingame = client.ingame
            ingame?.print(message.substring(5))
        } else if (message.startsWith("disconnect/")) {
            val serverKickReason = message.replace("disconnect/", "")
            Connection.logger.info(serverKickReason)
            close("Disconnected by server: \n$serverKickReason")
        }

        return false
    }

    override fun pushPacket(packet: Packet) {
        /*try {
            sendQueue!!.queue(encoderDecoder.buildOutgoingPacket(packet))
        } catch (e: UnknowPacketException) {
            Connection.logger.error("Couldn't pushPacket()", e)
        } catch (e: IOException) {
            close("Failed to push packet")
        }*/
        TODO()
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
