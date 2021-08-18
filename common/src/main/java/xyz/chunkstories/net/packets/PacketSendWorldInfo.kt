//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.Engine
import xyz.chunkstories.api.net.Packet
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.UserConnection
import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.serializeWorldInfo
import java.io.DataInputStream
import java.io.DataOutputStream

open class PacketSendWorldInfo(engine: Engine) : Packet(engine) {
    var worldInfo: World.Properties? = null

    constructor(engine: Engine, info: World.Properties?) : this(engine) {
        worldInfo = info
    }

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(serializeWorldInfo(worldInfo!!, false))
    }

    override fun receive(dis: DataInputStream, player: UserConnection?) {
        throw UnsupportedOperationException()
    }
}