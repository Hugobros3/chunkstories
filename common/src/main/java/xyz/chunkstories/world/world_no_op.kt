package xyz.chunkstories.world

import org.joml.Vector3dc
import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.sound.SoundSourceID

class NoOpParticlesManager : ParticlesManager {
    override fun <T : ParticleType.Particle> spawnParticle(typeName: String, init: T.() -> Unit) {}
    override fun <T : ParticleType.Particle> spawnParticle(type: ParticleTypeDefinition, init: T.() -> Unit) {}
}

class NoOpDecalsManager : DecalsManager {
    override fun add(position: Vector3dc, orientation: Vector3dc, size: Vector3dc, decalName: String) {}
}

class NoOpSoundManager : SoundManager {
    override fun playSoundEffect(soundEffect: String, mode: SoundSource.Mode, position: Vector3dc?, pitch: Float, gain: Float, attenuationStart: Float, attenuationEnd: Float) = null

    override fun getSoundSource(id: SoundSourceID) = null

    override fun stopAllSounds() {}

    override val playingSounds: Collection<SoundSource>
        get() = emptyList()

}