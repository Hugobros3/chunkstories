//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.content.translator.AbstractContentTranslator
import java.io.DataInputStream
import java.io.DataOutputStream

open class PacketContentTranslator : Packet {
    constructor()
    constructor(contentTranslator: AbstractContentTranslator) {
        serializedText = contentTranslator.toString(true)
    }

    protected lateinit var serializedText: String

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(serializedText)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        throw UnsupportedOperationException()
    }
}