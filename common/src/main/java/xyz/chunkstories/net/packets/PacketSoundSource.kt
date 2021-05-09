//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.net.packets

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.net.PacketWorld
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldSub
import xyz.chunkstories.sound.source.SoundSourceVirtual
import java.io.DataInputStream
import java.io.DataOutputStream

class PacketSoundSource(world: World) : PacketWorld(world) {
    var soundSourceToSend: SoundSourceVirtual? = null

    constructor(world: World, soundSource: SoundSourceVirtual) : this(world) {
        soundSourceToSend = soundSource
    }

    override fun send(dos: DataOutputStream) {
        dos.writeUTF(soundSourceToSend!!.soundName)
        dos.writeLong(soundSourceToSend!!.id)
        val position = soundSourceToSend!!.position
        if (position != null) {
            dos.writeBoolean(true)
            dos.writeFloat(position.x().toFloat())
            dos.writeFloat(position.y().toFloat())
            dos.writeFloat(position.z().toFloat())
        } else dos.writeBoolean(false)
        dos.writeByte(soundSourceToSend!!.mode.ordinal)
        dos.writeBoolean(soundSourceToSend!!.isDonePlaying)
        dos.writeFloat(soundSourceToSend!!.pitch)
        dos.writeFloat(soundSourceToSend!!.gain)
        dos.writeFloat(soundSourceToSend!!.attenuationStart)
        dos.writeFloat(soundSourceToSend!!.attenuationEnd)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        val soundName = dis.readUTF()
        val sourceId = dis.readLong()
        val hasPosition = dis.readBoolean()
        var position: Vector3dc? = null
        if (hasPosition) {
            position = Vector3d(dis.readFloat().toDouble(), dis.readFloat().toDouble(), dis.readFloat().toDouble())
        }
        val modeByte = dis.readByte()
        val mode = SoundSource.Mode.values()[modeByte.toInt()]
        val stopped = dis.readBoolean()
        val pitch = dis.readFloat()
        val gain = dis.readFloat()
        val attenuationStart = dis.readFloat()
        val attenuationEnd = dis.readFloat()

        val worldClient = world as? WorldSub ?: throw Exception("Not a client !")
        val client = worldClient.gameInstance as Client

        var soundSource: SoundSource? = client.soundManager.getSoundSource(sourceId)

        if (soundSource == null && stopped) return
        if (soundSource == null) {
            soundSource = client.soundManager.replicateServerSoundSource(soundName, mode, position!!, pitch, gain, attenuationStart, attenuationEnd, sourceId)
            return
        }
        if (stopped) {
            soundSource.stop()
            return
        }

        // Update the soundSource with all we can
        soundSource.position = position
        soundSource.pitch = pitch
        soundSource.gain = gain
        soundSource.attenuationStart = attenuationStart
        soundSource.attenuationEnd = attenuationEnd
    }
}