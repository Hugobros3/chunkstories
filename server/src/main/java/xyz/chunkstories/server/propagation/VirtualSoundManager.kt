//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.propagation

import org.joml.Vector3dc
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.sound.SoundSourceID
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.net.packets.PacketSoundSource
import xyz.chunkstories.server.player.ServerPlayer
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class VirtualSoundManager(private val worldServer: WorldMaster) : SoundManager {
    private val allPlayingSoundSources = mutableListOf<WeakReference<SoundSourceVirtual>>()
    private val playersSoundManagers = ConcurrentHashMap.newKeySet<ServerPlayerVirtualSoundManager>()

    inner class ServerPlayerVirtualSoundManager(internal val serverPlayer: ServerPlayer) : SoundManager {

        // As above, individual players have their playing sound sources kept track of
        internal var playingSoundSources = mutableListOf<WeakReference<SoundSourceVirtual>>()

        init {
            playersSoundManagers.add(this)
        }

        internal fun update() {
            // Stops caring when the player is disconnected
            /*if (!serverPlayer.isConnected) {
                playersSoundManagers.remove(this)
            }*/
            //TODO()
        }

        internal fun addSourceToPlayer(soundSource: SoundSourceVirtual) {
            playingSoundSources.add(WeakReference(soundSource))
        }

        override fun playSoundEffect(soundEffect: String, mode: Mode, position: Vector3dc?, pitch: Float, gain: Float, attStart: Float, attEnd: Float): SoundSource {
            val soundSource = SoundSourceVirtual(this@VirtualSoundManager, soundEffect, mode, position!!, pitch, gain, attStart, attEnd)
            // Play the sound effect for everyone
            addSourceToEveryone(soundSource, this)
            return soundSource
        }

        override fun getSoundSource(id: SoundSourceID): SoundSource? {
            for(ref in playingSoundSources) {
                val source = ref.get() ?: continue
                if(source.id == id)
                    return source
            }
            return null
        }

        fun stopAllSounds(soundEffect: String) {
            for (source in playingSounds) {
                if (source.soundName.contains(soundEffect)) {
                    source.stop()
                }
            }
        }

        override fun stopAllSounds() {
            for (soundSource in playingSounds) {
                soundSource.stop()
            }
        }

        override val playingSounds: Collection<SoundSource>
            get() = allPlayingSoundSources.mapNotNull { it.get() }.filter { !it.isDonePlaying }.toSet()

        internal fun couldHearSource(soundSource: SoundSourceVirtual): Boolean {
            if (soundSource.position == null)
                return true

            if (soundSource.gain <= 0)
                return true

            // special case, we want to make sure the players always know when we shut up a source
            if (soundSource.isDonePlaying)
                return true


            // Null location == Not spawned == No positional sounds for you!
            val loc = serverPlayer.state.location ?: return false
            return loc.distance(soundSource.position!!) < soundSource.attenuationEnd + 1.0
        }

        //TODO call this
        fun cleanup() {
            playersSoundManagers.remove(this)
        }
    }

    fun update() {
        // System.out.println("update srv sounds");
        val i = playersSoundManagers.iterator()
        while (i.hasNext()) {
            val playerSoundManager = i.next()
            playerSoundManager.update()
        }
    }

    private fun addSourceToEveryone(soundSource: SoundSourceVirtual, exceptHim: ServerPlayerVirtualSoundManager?) {
        // Create the sound creation packet
        val packet = PacketSoundSource(worldServer, soundSource)

        val i = playersSoundManagers.iterator()
        while (i.hasNext()) {
            val playerSoundManager = i.next()
            // Send it to all players than could hear it
            if (exceptHim == null || playerSoundManager != exceptHim) {
                if (!playerSoundManager.couldHearSource(soundSource))
                    continue

                // Creates the soundSource and adds it weakly to the player's list
                playerSoundManager.serverPlayer.pushPacket(packet)
                // TODO maybe not relevant since for updating we iterate over all players then
                // do a distance check
                playerSoundManager.addSourceToPlayer(soundSource)
            }
        }

        allPlayingSoundSources.add(WeakReference(soundSource))
    }

    fun updateSourceForEveryone(soundSource: SoundSourceVirtual, exceptHim: ServerPlayerVirtualSoundManager?) {
        // Create the update packet
        val packet = PacketSoundSource(worldServer, soundSource)

        for (playerSoundManager in playersSoundManagers) {
            // Send it to all players than could hear it
            if (playerSoundManager != exceptHim) {
                if (!playerSoundManager.couldHearSource(soundSource))
                    continue

                // Updates the soundSource
                playerSoundManager.serverPlayer.pushPacket(packet)
            }
        }
    }

    override fun playSoundEffect(soundEffect: String, mode: Mode, position: Vector3dc?, pitch: Float, gain: Float, attenuationStart: Float, attenuationEnd: Float): SoundSource {
        val soundSource = SoundSourceVirtual(this@VirtualSoundManager, soundEffect, mode, position!!, pitch, gain, attenuationStart, attenuationEnd)
        addSourceToEveryone(soundSource, null)
        return soundSource
    }

    fun stopAnySound(soundEffect: String) {
        for (soundSource in playingSounds) {
            if (soundSource.soundName.contains(soundEffect)) {
                soundSource.stop()
            }
        }
    }

    override fun stopAllSounds() {
        for (soundSource in playingSounds) {
            soundSource.stop()
        }
    }

    override fun getSoundSource(id: SoundSourceID): SoundSource? {
        for(ref in allPlayingSoundSources) {
            val source = ref.get() ?: continue
            if(source.id == id)
                return source
        }
        return null
    }

    override val playingSounds: Collection<SoundSource>
        get() = allPlayingSoundSources.mapNotNull { it.get() }.filter { !it.isDonePlaying }.toSet()

}
