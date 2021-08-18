//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import org.slf4j.LoggerFactory
import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.exceptions.net.IllegalPacketException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.api.world.WorldSub
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldSubImplementation

import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

typealias DownloadStartAction = (Connection.DownloadStatus) -> Unit

abstract class Connection(val engine: Engine, val remoteAddress: String, val port: Int) {

    abstract val encoderDecoder: PacketsEncoderDecoder

    abstract val isOpen: Boolean

    // Below is stuff for downloading/uploading of files
    private val fileStreamingRequests = ConcurrentHashMap<String, PendingDownload>()

    abstract fun connect(): Boolean

    abstract fun flush()

    abstract val world: WorldImplementation?
    abstract val userConnection: UserConnection?

    @Throws(IOException::class, PacketProcessingException::class, IllegalPacketException::class)
    fun handleDatagram(datagram: LogicalPacketDatagram) {
        val definition = datagram.packetDefinition as PacketDefinition
        if (definition.constructorTakesWorld) {
            val world = world
            if (world == null)
                logger.error("Received packet $definition but no world is up yet !")

            if(world is WorldSubImplementation)
                world.queueDatagram(datagram)
        } else {
            val packet = definition.createNewWithEngine(engine is Client, engine)
            packet!!.receive(datagram.data, userConnection)
            if (packet is PacketText) {
                handleSystemRequest(packet.text)
            }
            datagram.dispose()
        }
    }

    abstract fun handleSystemRequest(message: String): Boolean

    open fun sendTextMessage(string: String) {
        val packet = PacketText(engine)
        packet.text = string
        pushPacket(packet)
    }

    abstract fun pushPacket(packet: Packet)

    abstract fun close(reason: String)

    /**
     * Hints the connection logic that a file with a certain tag is to be expected,
     * and provides it with a location to save it. Unexpected file streaming will be
     * discarded.
     */
    fun registerExpectedFileStreaming(fileTag: String, whereToSave: File, action: DownloadStartAction) {
        if (fileStreamingRequests.putIfAbsent(fileTag, PendingDownload(whereToSave, action)) != null) {
            logger.warn("Requesting twice file: $fileTag")
        }
    }

    /**
     * Retreives and removes the expected location for the specified fileTag
     */
    fun getLocationForExpectedFile(fileTag: String): PendingDownload? {
        return fileStreamingRequests.remove(fileTag)
    }

    interface DownloadStatus {
        fun bytesDownloaded(): Int
        fun totalBytes(): Int
        fun waitsUntilDone(): Boolean
    }

    inner class PendingDownload(val f: File, val a: DownloadStartAction)

    companion object {
        val logger = LoggerFactory.getLogger("net.connection")
    }
}
