//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.exceptions.net.IllegalPacketException
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.packets.PacketText

import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

typealias DownloadStartAction = (Connection.DownloadStatus) -> Unit

abstract class Connection(val remoteAddress: String, val port: Int) {

    abstract val encoderDecoder: PacketsEncoderDecoder

    abstract val isOpen: Boolean

    // Below is stuff for downloading/uploading of files
    private val fileStreamingRequests = ConcurrentHashMap<String, PendingDownload>()

    abstract fun connect(): Boolean

    abstract fun flush()

    @Throws(IOException::class, PacketProcessingException::class, IllegalPacketException::class)
    abstract fun handleDatagram(datagram: LogicalPacketDatagram)

    abstract fun handleSystemRequest(msg: String): Boolean

    open fun sendTextMessage(string: String) {
        val packet = PacketText()
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

        /**
         * Waits for the download to end and returns true if successful
         */
        fun waitsUntilDone(): Boolean
    }

    inner class PendingDownload(val f: File, val a: DownloadStartAction)

    companion object {
        val logger = LoggerFactory.getLogger("net.connection")
    }
}
