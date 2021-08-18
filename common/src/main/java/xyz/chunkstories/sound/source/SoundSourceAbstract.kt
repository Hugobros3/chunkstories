//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound.source

import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.sound.SoundData
import org.joml.Vector3dc
import xyz.chunkstories.api.sound.SoundSourceID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

abstract class SoundSourceAbstract(
        soundData: SoundData,
        position: Vector3dc? = null,
        mode: SoundSource.Mode = SoundSource.Mode.NORMAL,
        pitch: Float = 1f,
        gain: Float = 1f,
        attenuationStart: Float = 1f,
        attenuationEnd: Float = 25f
) : SoundSource {
    override val id: SoundSourceID = -1
    override val soundName = soundData.name!!

    override var position: Vector3dc? by Delegates.observable(position, ::dirty)
    override var mode: SoundSource.Mode by Delegates.observable(mode, ::dirty)

    override var pitch: Float by Delegates.observable(pitch, ::dirty)
    override var gain: Float by Delegates.observable(gain, ::dirty)

    override var attenuationStart: Float by Delegates.observable(attenuationStart, ::dirty)
    override var attenuationEnd: Float by Delegates.observable(attenuationEnd, ::dirty)

    /** Keeps track of how many changes have been made to the source */
    protected var changes = AtomicInteger(1)

    protected open fun dirty(a: Any, old: Any?, new: Any?) {
        changes.incrementAndGet()
    }

    abstract override fun stop()
}