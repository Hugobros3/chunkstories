//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.packets

import xyz.chunkstories.api.content.json.stringSerialize
import xyz.chunkstories.api.content.json.toJson
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldSub
import xyz.chunkstories.api.world.cell.PodCellData
import xyz.chunkstories.content.translator.AbstractContentTranslator
import java.io.DataInputStream
import java.io.DataOutputStream

class PacketUpdateBlock : PacketWorld {
    private lateinit var cell: ChunkCell

    constructor(world: World) : super(world)
    constructor(context: ChunkCell) : super(context.world) {
        this.cell = context
    }

    override fun send(dos: DataOutputStream) {
        dos.writeInt(cell.x)
        dos.writeInt(cell.y)
        dos.writeInt(cell.z)

        val translator = world.gameInstance.contentTranslator as AbstractContentTranslator
        val blockID = translator.getIdForVoxel(cell.data.blockType)

        dos.writeInt(blockID)

        for ((name, additionalData) in cell.additionalData) {
            val serializedData = additionalData.serialize(world.gameInstance) ?: continue
            dos.writeByte(0x01.toByte().toInt())
            dos.writeUTF(name)
            dos.writeUTF(serializedData.stringSerialize())
        }
        dos.writeByte(0x00.toByte().toInt())
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        if (world is WorldSub) {
            val x = dis.readInt()
            val y = dis.readInt()
            val z = dis.readInt()

            val blockID = dis.readInt()
            val sunlight = dis.readInt()
            val blocklight = dis.readInt()
            val extraData = dis.readInt()

            val block = world.gameInstance.contentTranslator.getVoxelForId(blockID)!!
            val data = PodCellData(block, sunlight, blocklight, extraData)

            // TODO this isn't exactly super robust
            world.setCellData(x, y, z, data)
            val cell = world.getCell(x, y, z) as ChunkCell

            var nextComponent = dis.readByte()
            while (nextComponent.toInt() != 0) {
                val name = dis.readUTF()
                cell.additionalData[name]!!.deserialize(world.gameInstance, dis.readUTF().toJson())
                nextComponent = dis.readByte()
            }

        }
    }
}
