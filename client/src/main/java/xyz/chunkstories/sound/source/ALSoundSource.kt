//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound.source

import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.sound.SoundData
import org.joml.Vector3dc
import org.lwjgl.openal.AL10.*
import java.util.concurrent.locks.ReentrantLock

open class ALSoundSource constructor(open val soundData: SoundData, position: Vector3dc?, mode: SoundSource.Mode, pitch: Float, gain: Float, attStart: Float, attEnd: Float) : SoundSourceAbstract(soundData, position, mode, pitch, gain, attStart, attEnd) {
    protected var openAlSourceId: Int = 0

    private val lock = ReentrantLock()
    private var soundStartTime: Long = 0

    fun play() {
        openAlSourceId = alGenSources()

        feedSourceInitialData()
        updateAlSourceProperties()

        alSourcePlay(openAlSourceId)

        soundStartTime = System.currentTimeMillis()
    }

    open fun feedSourceInitialData() {
        alSourcei(openAlSourceId, AL_BUFFER, soundData.buffer)}

    open fun update(manager: SoundManager) {
        updateAlSourceProperties()
    }

    override val isDonePlaying: Boolean
        get() = mode != SoundSource.Mode.LOOPED && System.currentTimeMillis() - soundStartTime > soundData.lengthMs

    override fun stop() {
        alSourceStop(openAlSourceId)
        cleanup()
    }

    open fun cleanup() {
        alDeleteSources(openAlSourceId)
    }

    private fun updateAlSourceProperties() {
        lock.lock()

        if (position != null)
            alSource3f(openAlSourceId, AL_POSITION, position!!.x().toFloat(), position!!.y().toFloat(), position!!.z().toFloat())

        val changesToApply = changes.get()
        if (changesToApply > 0) {
            alSourcef(openAlSourceId, AL_PITCH, pitch)
            alSourcef(openAlSourceId, AL_GAIN, gain)

            val isAmbient = this.position == null
            if (isAmbient) {
                alSourcei(openAlSourceId, AL_SOURCE_RELATIVE, AL_TRUE)
                alSource3f(openAlSourceId, AL_POSITION, 0.0f, 0.0f, 0.0f)
                alSource3f(openAlSourceId, AL_VELOCITY, 0.0f, 0.0f, 0.0f)
            }
            alSourcei(openAlSourceId, AL_ROLLOFF_FACTOR, if (isAmbient) 0 else 1)

            alSourcef(openAlSourceId, AL_REFERENCE_DISTANCE, attenuationStart)
            alSourcef(openAlSourceId, AL_MAX_DISTANCE, attenuationEnd)

            changes.addAndGet(-changesToApply)
        }
        lock.unlock()
    }
}
