//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

import xyz.chunkstories.api.net.PacketDestinator
import xyz.chunkstories.api.net.PacketReceptionContext
import xyz.chunkstories.api.net.PacketSender
import xyz.chunkstories.api.net.PacketSendingContext
import xyz.chunkstories.api.net.PacketWorldStreaming
import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.chunk.ChunkCompressedData

class PacketChunkCompressedData : PacketWorldStreaming {

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

        this.data = data
    }

    @Throws(IOException::class)
    override fun send(destinator: PacketDestinator, dos: DataOutputStream, ctx: PacketSendingContext) {
        dos.writeInt(x)
        dos.writeInt(y)
        dos.writeInt(z)

        data.toBytes(dos)
    }

    @Throws(IOException::class)
    override fun process(sender: PacketSender, dis: DataInputStream, processor: PacketReceptionContext) {
        x = dis.readInt()
        y = dis.readInt()
        z = dis.readInt()

        data = ChunkCompressedData.fromBytes(dis)
    }
}
