//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.source

import org.joml.Vector3dc

import io.xol.chunkstories.api.sound.SoundSource

class DummySoundSource : SoundSource {
    override var attenuationEnd = 0f
    override var attenuationStart = 0f
    override var gain = 0f
    override val isDonePlaying = true
    override val mode: SoundSource.Mode = SoundSource.Mode.NORMAL
    override val name: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override var pitch = 1f
    override var position: Vector3dc? = null
    override val uuid: Long = -1

    override fun stop() {
        
    }


}
