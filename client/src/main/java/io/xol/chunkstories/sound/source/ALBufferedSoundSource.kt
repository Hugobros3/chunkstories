//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.source

import io.xol.chunkstories.api.sound.SoundManager
import io.xol.chunkstories.api.sound.SoundSource
import io.xol.chunkstories.sound.SoundDataBuffered
import org.joml.Vector3dc
import org.lwjgl.openal.AL10

class ALBufferedSoundSource
constructor(override val soundData: SoundDataBuffered, position: Vector3dc?, pitch: Float, gain: Float, attStart: Float, attEnd: Float) :
        ALSoundSource(soundData, position, SoundSource.Mode.STREAMED, pitch, gain, attStart, attEnd) {

    override fun feedSourceInitialData() {
        // Upload the first two pages, the first one is set to be the first one we'll swap
        AL10.alSourceQueueBuffers(openAlSourceId, soundData.uploadNextPage(openAlSourceId))
        AL10.alSourceQueueBuffers(openAlSourceId, soundData.uploadNextPage(openAlSourceId))
    }

    override fun update(manager: SoundManager) {

        // Update buffered sounds
        // Gets how many buffers we read entirely
        var elapsed = AL10.alGetSourcei(openAlSourceId, AL10.AL_BUFFERS_PROCESSED)
        while (elapsed > 0) {
            // Get rid of them
            val removeMeh = AL10.alSourceUnqueueBuffers(openAlSourceId)
            AL10.alDeleteBuffers(removeMeh)
            // Queue a new one
            AL10.alSourceQueueBuffers(openAlSourceId, soundData.uploadNextPage(openAlSourceId))
            elapsed--
        }

        super.update(manager)
    }

    override fun cleanup() {
        var elapsed = AL10.alGetSourcei(openAlSourceId, AL10.AL_BUFFERS_PROCESSED)
        while (elapsed > 0) {
            val removeMeh = AL10.alSourceUnqueueBuffers(openAlSourceId)
            AL10.alDeleteBuffers(removeMeh)
            elapsed--
        }

        soundData.destroy()

        super.cleanup()
    }
}
