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
import java.io.DataInputStream
import java.io.DataOutputStream

// TODO: Use ContentTranslator to assign ids to particles
// TODO: Use reflection or something and send the raw fields of the Particle
// object
class PacketParticle : PacketWorld {
    private var particleName = ""
    private var position: Vector3dc? = null
    private var velocity: Vector3dc? = null

    constructor(world: World) : super(world)
    constructor(world: World, particleName: String, position: Vector3dc?, velocity: Vector3dc?) : super(world) {
        this.particleName = particleName
        this.position = position
        this.velocity = velocity
    }

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(particleName)
        dos.writeDouble(position!!.x())
        dos.writeDouble(position!!.y())
        dos.writeDouble(position!!.z())
        dos.writeBoolean(velocity != null)
        if (velocity != null) {
            dos.writeDouble(velocity!!.x())
            dos.writeDouble(velocity!!.y())
            dos.writeDouble(velocity!!.z())
        }
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        particleName = dis.readUTF()
        val position = Vector3d()
        position.x = dis.readDouble()
        position.y = dis.readDouble()
        position.z = dis.readDouble()
        val velocity = Vector3d()
        if (dis.readBoolean()) {
            velocity.x = dis.readDouble()
            velocity.y = dis.readDouble()
            velocity.z = dis.readDouble()
        }
    }
}