//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldClient
import java.io.DataInputStream
import java.io.DataOutputStream

/** Simply sends a decal to the client to be drawn  */
class PacketDecal : PacketWorld {
    private lateinit var decalName: String
    private lateinit var position: Vector3dc
    private lateinit var orientation: Vector3dc
    private lateinit var size: Vector3dc

    constructor(world: World) : super(world)
    constructor(world: World, decalName: String, position: Vector3dc, orientation: Vector3dc, size: Vector3dc) : super(world) {
        this.decalName = decalName
        this.position = position
        this.orientation = orientation
        this.size = size
    }

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(decalName)
        dos.writeDouble(position.x())
        dos.writeDouble(position.y())
        dos.writeDouble(position.z())
        dos.writeDouble(orientation.x())
        dos.writeDouble(orientation.y())
        dos.writeDouble(orientation.z())
        dos.writeDouble(size.x())
        dos.writeDouble(size.y())
        dos.writeDouble(size.z())
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        decalName = dis.readUTF()
        val position = Vector3d()
        position.x = dis.readDouble()
        position.y = dis.readDouble()
        position.z = dis.readDouble()
        val orientation = Vector3d()
        orientation.x = dis.readDouble()
        orientation.y = dis.readDouble()
        orientation.z = dis.readDouble()
        val size = Vector3d()
        size.x = dis.readDouble()
        size.y = dis.readDouble()
        size.z = dis.readDouble()
        if (world is WorldClient) {
            world.decalsManager.add(position, orientation, size, decalName)
        }
    }
}