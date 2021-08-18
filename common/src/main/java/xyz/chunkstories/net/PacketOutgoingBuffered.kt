//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net

import xyz.chunkstories.api.net.PacketId
import java.io.DataOutputStream

class PacketOutgoingBuffered(val context: PacketsEncoderDecoder, val id: PacketId, val size: Int, val payload: ByteArray) : PacketOutgoing {

    private fun writePacketIdHeader(out: DataOutputStream) {
        if (id < 127) out.writeByte(id) else {
            out.writeByte(0x80 or (id shr 8))
            out.writeByte(id and 0xFF)
        }
    }

    override fun write(out: DataOutputStream) {
        writePacketIdHeader(out)
        out.writeInt(size)
        out.write(payload)
    }
}