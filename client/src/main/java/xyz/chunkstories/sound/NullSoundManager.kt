package xyz.chunkstories.sound

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3fc
import xyz.chunkstories.api.client.ClientSoundManager
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource

class NullSoundManager : ClientSoundManager {
    override fun setListenerPosition(position: Vector3fc?, lookAt: Vector3fc?, up: Vector3fc?) {
    }

    override fun getAllPlayingSounds(): MutableCollection<SoundSource> = mutableListOf()

    override fun stopAnySound(soundEffect: String?) {
    }

    override fun stopAnySound() {
    }

    override fun playSoundEffect(soundEffect: String?, mode: SoundSource.Mode?, position: Vector3dc?, pitch: Float, gain: Float, attStart: Float, attEnd: Float): SoundSource {
        return NullSoundSource()
    }

    override fun replicateServerSoundSource(soundName: String, mode: SoundSource.Mode, position: Vector3dc, pitch: Float, gain: Float, attenuationStart: Float, attenuationEnd: Float, UUID: Long): SoundSource {
        return NullSoundSource()
    }
}

class NullSoundSource : SoundSource {
    override var attenuationEnd: Float = 0f
    override var attenuationStart: Float = 0f
    override var gain: Float = 0f
    override val isDonePlaying: Boolean = true
    override val mode: SoundSource.Mode = SoundSource.Mode.NORMAL
    override val name: String = "dummy source"
    override var pitch: Float = 1f
    override var position: Vector3dc? = Vector3d(0.0)
    override val uuid: Long = -1
    override fun stop() {}
}