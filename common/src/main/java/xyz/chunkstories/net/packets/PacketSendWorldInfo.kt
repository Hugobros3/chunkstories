//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.serializeWorldInfo
import java.io.DataInputStream
import java.io.DataOutputStream

open class PacketSendWorldInfo : Packet {
    var worldInfo: World.Properties? = null

    constructor()
    constructor(info: World.Properties?) {
        worldInfo = info
    }

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(serializeWorldInfo(worldInfo!!, false))
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        throw UnsupportedOperationException()
    }
}