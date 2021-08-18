//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.vanillasockets

import org.slf4j.LoggerFactory
import xyz.chunkstories.net.Connection
import java.lang.Thread
import java.net.SocketException
import java.io.DataInputStream
import java.lang.Exception

/** Eats what a InputStream provides and digests it  */
abstract class StreamGobbler(private val connection: Connection, private val `in`: DataInputStream) : Thread() {
    override fun run() {
        try {
            while (connection.isOpen) {
                val datagram = connection.encoderDecoder.digestIncommingPacket(`in`)
                connection.handleDatagram(datagram)
            }
        } catch (e: SocketException) {
            connection.close("Closen socket")
            if (connection.isOpen) {
                logger.info("Closing socket.")
            }
        } catch (e: Exception) {
            connection.close("EOFException")
            e.printStackTrace()
            logger.info("Connection closed")
        }
    }

    companion object {
        protected val logger = LoggerFactory.getLogger("client.net.connection.in")
    }
}