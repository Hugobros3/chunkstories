//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.packets

import xyz.chunkstories.api.Engine
import java.io.DataInputStream
import java.io.IOException

import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.api.server.UserConnection

class PacketReceiveText(engine: Engine) : PacketText(engine) {
    @Throws(IOException::class)

    override fun receive(dis: DataInputStream, user: UserConnection?) {
        super.receive(dis, user)
        TODO()
        // (processor as ClientPacketsEncoderDecoder).connection.handleSystemRequest(text)
    }
}
