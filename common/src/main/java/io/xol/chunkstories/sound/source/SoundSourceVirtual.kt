//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.source

import io.xol.chunkstories.sound.DummySoundData
import org.joml.Vector3dc

import io.xol.chunkstories.api.sound.SoundSource
import io.xol.chunkstories.sound.VirtualSoundManager

/**
 * Server-side version of a soundSource
 */
class SoundSourceVirtual(private val virtualServerSoundManager: VirtualSoundManager, soundEffect: String, mode: SoundSource.Mode,
                         position: Vector3dc, pitch: Float, gain: Float, attStart: Float, attEnd: Float) : SoundSourceAbstract(DummySoundData(soundEffect), position, mode, pitch, gain, attStart, attEnd) {

    override var isDonePlaying = false

    override fun stop() {
        isDonePlaying = true
        stop()
    }

    override fun dirty(a: Any, old: Any?, o: Any?) {
        virtualServerSoundManager.updateSourceForEveryone(this, null)
        super.dirty(a, old, o)
    }

}
