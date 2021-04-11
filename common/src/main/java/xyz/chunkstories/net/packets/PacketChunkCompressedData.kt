//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import java.io.DataInputStream
import java.io.DataOutputStream

import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.chunk.ChunkCompressedData

class PacketChunkCompressedData : PacketWorld {

    var x: Int = 0
    var y: Int = 0
    var z: Int = 0

    lateinit var data: ChunkCompressedData

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(chunk: ChunkImplementation, data: ChunkCompressedData) : super(chunk.world) {
        this.x = chunk.chunkX
        this.y = chunk.chunkY
        this.z = chunk.chunkZ

        this.data = data.stripEntities()
    }

    override fun send(dos: DataOutputStream) {
        dos.writeInt(x)
        dos.writeInt(y)
        dos.writeInt(z)

        data.toBytes(dos)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        x = dis.readInt()
        y = dis.readInt()
        z = dis.readInt()

        data = ChunkCompressedData.fromBytes(dis)
    }
}
