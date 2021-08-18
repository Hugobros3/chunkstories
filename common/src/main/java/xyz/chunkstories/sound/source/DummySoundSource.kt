//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound.source

import org.joml.Vector3dc

import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.sound.SoundSourceID

class DummySoundSource : SoundSource {
    override var attenuationEnd = 0f
    override var attenuationStart = 0f
    override var gain = 0f
    override val isDonePlaying = true
    override val mode: SoundSource.Mode = SoundSource.Mode.NORMAL
    override val soundName: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override var pitch = 1f
    override var position: Vector3dc? = null
    override val id: SoundSourceID = -1

    override fun stop() {
        
    }


}
