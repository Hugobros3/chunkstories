//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.sound

import org.joml.Vector3dc
import org.joml.Vector3fc
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.net.packets.PacketSoundSource
import xyz.chunkstories.sound.source.SoundSourceVirtual
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class VirtualSoundManager(private val worldServer: WorldMaster)// this.server = server;
    : SoundManager {

    // We weakly keep track of playing sounds, the server has no idea of the actual
    // sound data and this does not keeps track of played sounds !
    private val allPlayingSoundSources = ConcurrentHashMap.newKeySet<WeakReference<SoundSourceVirtual>>()

    // Each player has his own instance of the soundManager
    private val playersSoundManagers = ConcurrentHashMap.newKeySet<ServerPlayerVirtualSoundManager>()

    inner class ServerPlayerVirtualSoundManager(internal var serverPlayer: Player) : SoundManager {

        // As above, individual players have their playing sound sources kept track of
        internal var playingSoundSources: MutableSet<WeakReference<SoundSourceVirtual>> = ConcurrentHashMap.newKeySet()

        init {
            playersSoundManagers.add(this)
        }

        internal fun update() {
            // Stops caring when the player is disconnected
            /*if (!serverPlayer.isConnected) {
                playersSoundManagers.remove(this)
            }*/
            TODO()
        }

        internal fun addSourceToPlayer(soundSource: SoundSourceVirtual) {
            playingSoundSources.add(WeakReference(soundSource))
        }

        override fun playSoundEffect(soundEffect: String, mode: Mode, position: Vector3dc?, pitch: Float, gain: Float,
                                     attStart: Float, attEnd: Float): SoundSource {
            val soundSource = SoundSourceVirtual(this@VirtualSoundManager, soundEffect, mode,
                    position!!, pitch, gain, attStart, attEnd)
            // Play the sound effect for everyone
            addSourceToEveryone(soundSource, this)
            return soundSource
        }

        override fun playSoundEffect(soundEffect: String): SoundSource? {
            return null
        }

        override fun stopAnySound(soundEffect: String) {
            for (s in allPlayingSounds) {
                if (s.name.contains(soundEffect)) {
                    s.stop()
                }
            }
        }

        override fun stopAnySound() {
            for (soundSource in allPlayingSounds) {
                soundSource.stop()
            }
        }

        /**
         * Alls sounds we kept track of playing for this player
         */
        override fun getAllPlayingSounds(): Set<SoundSource> {
            return allPlayingSoundSources.mapNotNull { it.get() }.filter { !it.isDonePlaying }.toSet()
            /*return new Iterator<SoundSource>() {
				Iterator<WeakReference<SoundSourceVirtual>> iterator = playingSoundSources.iterator();
				SoundSource next = null;

				@Override
				public boolean hasNext() {
					if (next != null)
						return true;

					while (iterator.hasNext() && next == null) {
						WeakReference<SoundSourceVirtual> weakReference = iterator.next();
						SoundSourceVirtual soundSource = weakReference.get();
						if (soundSource == null || soundSource.isDonePlaying()) {
							iterator.remove();
							continue;
						}

						next = soundSource;
					}

					return false;
				}

				@Override
				public SoundSource next() {
					if (next == null)
						hasNext();

					SoundSource oldNext = next;
					next = null;
					return oldNext;
				}

			};*/
        }

        override fun setListenerPosition(position: Vector3fc, lookAt: Vector3fc, up: Vector3fc) {
            throw UnsupportedOperationException("Irrelevant")
        }

        internal fun couldHearSource(soundSource: SoundSourceVirtual): Boolean {
            if (soundSource.position == null)
                return true

            if (soundSource.gain <= 0)
                return true

            // special case, we want to make sure the players always know when we shut up a source
            if (soundSource.isDonePlaying)
                return true

            // Null location == Not spawned == No positional sounds for you!
            val loc = serverPlayer.controlledEntity?.location ?: return false

            return loc.distance(soundSource.position!!) < soundSource.attenuationEnd + 1.0
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

    override fun playSoundEffect(soundEffect: String, mode: Mode, position: Vector3dc?, pitch: Float, gain: Float,
                                 attStart: Float, attEnd: Float): SoundSource {
        val soundSource = SoundSourceVirtual(this@VirtualSoundManager, soundEffect, mode, position!!,
                pitch, gain, attStart, attEnd)
        // Play the sound effect for everyone
        addSourceToEveryone(soundSource, null)
        return soundSource
    }

    /*
	 * @Override public SoundSource playSoundEffect(String soundEffect) { // TODO
	 * Have yet to specify on playing GUI sounds for everyone return null; }
	 */

    override fun stopAnySound(soundEffect: String) {
        for (s in allPlayingSounds) {
            if (s.name.contains(soundEffect)) {
                s.stop()
            }
        }
    }

    override fun stopAnySound() {
        for (soundSource in allPlayingSounds) {
            soundSource.stop()
        }
    }

    override fun setListenerPosition(position: Vector3fc, lookAt: Vector3fc, up: Vector3fc) {}

    override fun getAllPlayingSounds(): Set<SoundSource> {
        return allPlayingSoundSources.mapNotNull { it.get() }.filter { !it.isDonePlaying }.toSet()
    }

}
