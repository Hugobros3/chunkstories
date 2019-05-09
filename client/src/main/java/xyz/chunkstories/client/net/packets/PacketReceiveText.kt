//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets

import java.io.DataInputStream
import java.io.IOException

import xyz.chunkstories.api.net.PacketReceptionContext
import xyz.chunkstories.api.net.PacketSender
import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.client.net.ClientPacketsEncoderDecoder

class PacketReceiveText : PacketText() {
    @Throws(IOException::class)
    override fun process(sender: PacketSender?, `in`: DataInputStream, processor: PacketReceptionContext?) {
        super.process(sender, `in`, processor)
        //(processor as ClientPacketsEncoderDecoder).connection.handleSystemRequest(text)
    }
}
