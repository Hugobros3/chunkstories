//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound

import java.util.HashMap

import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.sound.ogg.SoundDataOggSample
import xyz.chunkstories.sound.ogg.SoundDataOggStream

class SoundsLibrary(private val client: ClientImplementation) {

    fun obtainSample(soundEffect: String?): SoundData? {
        if (soundEffect == null)
            return null

        return soundsData.getOrPut(soundEffect) {
            if (soundEffect.endsWith(".ogg")) {
               loadOggSample(soundEffect)
            } else throw Exception("Unhandled extension: $soundEffect")
        }
    }

    private fun loadOggSample(sampleName: String): SoundData {
        val sampleData = SoundDataOggSample(client.content.getAsset(sampleName))
        sampleData.name = sampleName
        return if (sampleData.loadedOk()) sampleData else throw Exception("Failed to load sample $sampleName")
    }

    fun obtainBufferedSample(musicName: String): SoundDataBuffered? {
        val soundDataBuffered: SoundDataBuffered?
        when {
            musicName.endsWith(".ogg") -> soundDataBuffered = obtainOggStream(musicName)
            else -> return null
        }
        return soundDataBuffered
    }

    private fun obtainOggStream(streamedSampleName: String): SoundDataBuffered? {
        val soundData = SoundDataOggStream(client.content.getAsset(streamedSampleName)!!.read())
        soundData.name = streamedSampleName
        return if (soundData.loadedOk()) soundData else throw Exception("Failed to open stream $streamedSampleName")

    }

    fun cleanup() {
        for (soundData in soundsData.values) {
            soundData.destroy()
        }
        soundsData.clear()
    }

    companion object {
        private val soundsData = HashMap<String, SoundData>()
    }
}
