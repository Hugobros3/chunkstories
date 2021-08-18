//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.content.translator.AbstractContentTranslator
import java.io.DataInputStream
import java.io.DataOutputStream

open class PacketContentTranslator(engine: Engine) : Packet(engine) {
    constructor(engine: Engine, contentTranslator: AbstractContentTranslator) : this(engine) {
        serializedText = contentTranslator.toString(true)
    }

    protected lateinit var serializedText: String

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(serializedText)
    }

    override fun receive(dis: DataInputStream, user: UserConnection?) {
        throw UnsupportedOperationException()
    }
}