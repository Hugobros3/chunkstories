//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import java.io.DataInputStream
import java.io.DataOutputStream

/** Packet the client sends to the server to tell him what he requests it to
 * load in. Server may answer with an UNREGISTER_CHUNK_... packet if we
 * requested a chunk that is too far away for us to be allowed to request it  */
open class PacketWorldUser : PacketWorld {
    lateinit var tag: Tag
        protected set
    var x = 0
        protected set
    var y = 0
        protected set
    var z = 0
        protected set

    enum class Tag {
        REGISTER_CHUNK, UNREGISTER_CHUNK, REGISTER_SUMMARY, UNREGISTER_SUMMARY
    }

    constructor(world: World) : super(world)
    private constructor(world: World, tag: Tag, x: Int, y: Int, z: Int) : super(world) {
        this.tag = tag
        this.x = x
        this.y = y
        this.z = z
    }

    override fun send(dos: DataOutputStream) {
        dos.writeByte(tag.ordinal)
        if (tag == Tag.REGISTER_SUMMARY || tag == Tag.UNREGISTER_SUMMARY) {
            dos.writeInt(x)
            dos.writeInt(z)
        } else {
            dos.writeInt(x)
            dos.writeInt(y)
            dos.writeInt(z)
        }
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        tag = Tag.values()[dis.readByte().toInt()]
        if (tag == Tag.REGISTER_SUMMARY || tag == Tag.UNREGISTER_SUMMARY) {
            x = dis.readInt()
            z = dis.readInt()
        } else {
            x = dis.readInt()
            y = dis.readInt()
            z = dis.readInt()
        }
    }

    companion object {
        fun registerChunkPacket(world: World, chunkX: Int, chunkY: Int, chunkZ: Int): PacketWorldUser {
            return PacketWorldUser(world, Tag.REGISTER_CHUNK, chunkX, chunkY, chunkZ)
        }

        fun unregisterChunkPacket(world: World, chunkX: Int, chunkY: Int, chunkZ: Int): PacketWorldUser {
            return PacketWorldUser(world, Tag.UNREGISTER_CHUNK, chunkX, chunkY, chunkZ)
        }

        fun registerSummary(world: World, regionX: Int, regionY: Int): PacketWorldUser {
            return PacketWorldUser(world, Tag.REGISTER_SUMMARY, regionX, 0, regionY)
        }

        fun unregisterSummary(world: World, regionX: Int, regionY: Int): PacketWorldUser {
            return PacketWorldUser(world, Tag.UNREGISTER_SUMMARY, regionX, 0, regionY)
        }
    }
}