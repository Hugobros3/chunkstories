//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.vanillasockets

import org.slf4j.LoggerFactory
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.PacketOutgoing
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * The job of this thread is to write datagrams to an output stream. Not much in
 * the way of complexity there
 */
class SendQueue(private val connection: Connection, private val outputStream: DataOutputStream) : Thread() {
    private val sendQueue = LinkedBlockingQueue<PacketOutgoing>()
    private val queueSemaphore = Semaphore(0)

    private val deathSemaphore = Semaphore(0)

    /** Special one that breaks the loop */
    internal var DIE: PacketOutgoing = PacketOutgoing { throw UnsupportedOperationException() }

    init {
        this.name = "Send queue thread"
    }

    internal inner class Flush : PacketOutgoing {

        @Throws(IOException::class)
        override fun write(out: DataOutputStream) {
            throw UnsupportedOperationException()
        }
    }

    fun queue(packet: PacketOutgoing) {
        sendQueue.add(packet)
        queueSemaphore.release()
    }

    fun flush() {
        val flush = Flush()
        sendQueue.add(flush)
        queueSemaphore.release()
    }

    override fun run() {
        while (true) {
            var packet: PacketOutgoing? = null

            try {
                packet = sendQueue.take()
            } catch (e1: InterruptedException) {
                e1.printStackTrace()
            }

            if (packet === DIE) {
                // Kill request ? accept gracefully our fate
                break
            }

            if (packet == null) {
                println("ASSERTION FAILED : THE SEND QUEUE CAN'T CONTAIN NULL PACKETS.")
                System.exit(-1)
            } else if (packet is Flush) {
                try {
                    outputStream.flush()
                    //packet.fence.signal()
                } catch (e: IOException) {
                    // That's basically terminated connection exceptions
                    //packet.fence.signal()
                    handleError("Unable to flush: " + e.message)
                    break
                }

            } else
                try {
                    packet.write(outputStream)
                } catch (e: IOException) {
                    // We don't care about that, it's the motd thing mostly
                    handleError("Unable to send packet: " + e.message)
                    break
                }

        }

        deathSemaphore.release()
    }

    private fun handleError(reason: String) {
        logger.error("Error in send queue: $reason")
        connection.close(reason)
    }

    fun shutdown() {
        sendQueue.add(DIE)
        queueSemaphore.release()

        // 5s grace time
        deathSemaphore.tryAcquire(5, TimeUnit.SECONDS)
        sendQueue.clear()
    }

    companion object {
        private val logger = LoggerFactory.getLogger("net")
    }
}
