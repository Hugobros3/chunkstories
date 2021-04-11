//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.world.heightmap.HeightmapImplementation
import xyz.chunkstories.world.heightmap.HeightmapImplementation.Companion.compressor
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

class PacketHeightmap : PacketWorld {
    lateinit var heightmap: HeightmapImplementation
	private var rx = 0
	private var rz = 0

    lateinit var compressedData: ByteArray

    constructor(world: World?) : super(world!!) {}
    constructor(heightmap: HeightmapImplementation) : super(heightmap.world) {
        rx = heightmap.regionX
        rz = heightmap.regionZ
        this.heightmap = heightmap
    }

    override fun send(dos: DataOutputStream) {
        dos.writeInt(rx)
        dos.writeInt(rz)
        val heights = heightmap.heightData
        val ids = heightmap.blockTypesData
        val compressMe = ByteBuffer.allocateDirect(256 * 256 * 4 * 2)
        for (i in 0 until 256 * 256) compressMe.putInt(heights[i])
        for (i in 0 until 256 * 256) compressMe.putInt(ids[i])
        compressMe.flip()
        val unCompressed = ByteArray(compressMe.remaining())
        compressMe[unCompressed]
        val compressedData = compressor.compress(unCompressed)
        dos.writeInt(compressedData.size)
        dos.write(compressedData)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        rx = dis.readInt()
        rz = dis.readInt()
        val dataLength = dis.readInt()
        compressedData = ByteArray(dataLength)
        dis.readFully(compressedData)

        // TODO
    }
}