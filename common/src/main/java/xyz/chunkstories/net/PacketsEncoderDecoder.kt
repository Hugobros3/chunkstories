//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.net.PacketId
import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.net.packets.PacketSendFile
import xyz.chunkstories.world.WorldImplementation
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * The task of the packet processor is to decode & sort incomming packets by ID
 * and to send outcoming packets with the right packet ID.
 */
abstract class PacketsEncoderDecoder(protected val store: PacketsStore, val connection: Connection) {

    lateinit var contentTranslator: AbstractContentTranslator
    abstract val world: WorldImplementation?

    /**
     * Read 1 or 2 bytes to get the next packet ID and returns a packet of this type
     * if it exists
     */
    fun digestIncommingPacket(dis: DataInputStream): LogicalPacketDatagram {
        val firstByte = dis.readByte().toInt()
        var packetTypeId = 0
        // If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
        if (firstByte and 0x80 == 0) packetTypeId = firstByte else {
            // It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
            var secondByte = dis.readByte().toInt()
            secondByte = secondByte and 0xFF
            packetTypeId = secondByte or (firstByte and 0x7F) shl 8
        }
        val def = contentTranslator.getPacketForId(packetTypeId) ?: throw Exception("Unknown packet id: $packetTypeId")
        if (def.isStreamed || def.name == "file") {
            return object : LogicalPacketDatagram(def, -1) {
                override fun getData(): DataInputStream {
                    return dis
                }

                override fun dispose() {}
            }
        }
        val packetLength = dis.readInt()
        val bitme = ByteArray(packetLength)
        dis.readFully(bitme)
        return PacketIngoingBuffered(def, packetLength, bitme)
    }

    private fun writePacketIdHeader(out: DataOutputStream, id: PacketId) {
        if (id < 127)
            out.writeByte(id)
        else {
            out.writeByte(0x80 or (id shr 8))
            out.writeByte(id and 0xFF)
        }
    }

    fun buildOutgoingPacket(packet: Packet): PacketOutgoing {
        try {
            val packetId = findIdForPacket(packet)
            if (packet is PacketSendFile) {
                return object : PacketOutgoing {
                    override fun write(out: DataOutputStream) {
                        writePacketIdHeader(out, packetId)
                        packet.send(out)
                    }
                }
            }
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            packet.send(dos)
            return PacketOutgoingBuffered(this, packetId, baos.size(), baos.toByteArray())
        } catch (e: Exception) {
            logger.error("Error : unable to buffer Packet $packet", e)
            throw e
        }
    }

    private fun findIdForPacket(packet: Packet): PacketId {
        val world = world
        return if (world == null) {
            when (packet) {
                is PacketText -> 0x00
                is PacketSendFile -> 0x01
                else -> throw RuntimeException("Cannot send this packet while not in a world")
            }
        } else {
            val def = world.content.packets.getPacketFromInstance(packet) ?: throw Exception("Could not find the definition of packet $packet")
            val id = world.contentTranslator.getIdForPacket(def)
            if (id == -1)
                throw Exception("Could not find the id of packet definition " + def.name)
            id
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger("net.packetsProcessor")
    }
}