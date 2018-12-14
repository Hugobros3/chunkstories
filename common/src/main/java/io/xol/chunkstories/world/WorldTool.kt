//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import java.io.File

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager
import io.xol.chunkstories.api.world.WorldInfo
import org.joml.Vector3dc
import org.joml.Vector3fc

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.particles.ParticlesManager
import io.xol.chunkstories.api.player.Player
import io.xol.chunkstories.api.sound.SoundManager
import io.xol.chunkstories.api.sound.SoundSource
import io.xol.chunkstories.api.sound.SoundSource.Mode
import io.xol.chunkstories.api.util.IterableIterator
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.sound.source.DummySoundSource
import io.xol.chunkstories.world.io.IOTasks

class WorldTool @Throws(WorldLoadingException::class)
constructor(gameContext: GameContext, info: WorldInfo, folder: File, immediateIO: Boolean) : WorldImplementation(gameContext, info, null, folder), WorldMaster {

    var isLightningEnabled = true
    var isGenerationEnabled = true

    override val ioHandler: IOTasks = IOTasks(this)

    override val soundManager: SoundManager
        get() = nullSoundManager

    private var nullSoundManager = NullSoundManager()

    override val particlesManager: ParticlesManager
        get() = nullParticlesManager

    private var nullParticlesManager = NullParticlesManager()

    override val decalsManager: DecalsManager
        get() = nullDecalsManager

    private var nullDecalsManager = NullDecalsManager()

    override val players: Set<Player>
        get() = throw UnsupportedOperationException("getPlayers")

    init {
        ioHandler.start()
    }

    companion object {
        fun GameContext.createWorld(folder: File, worldInfo: WorldInfo) : WorldTool {
            if(folder.exists())
                throw Exception("The folder $folder already exists !")

            logger().debug("Creating new world")
            folder.mkdirs()
            val worldInfoFile = java.io.File(folder.path + "/worldInfo.dat")
            worldInfoFile.writeText(io.xol.chunkstories.world.serializeWorldInfo(worldInfo, true))
            logger().debug("Created directory & wrote worldInfo.dat ; now entering world")

            return WorldTool(this, worldInfo, folder, false)
        }
    }

    internal inner class NullSoundManager : SoundManager {

        override fun playSoundEffect(soundEffect: String): SoundSource? {

            return null
        }

        override fun stopAnySound(soundEffect: String) {

        }

        override fun stopAnySound() {}

        override fun getAllPlayingSounds(): Set<SoundSource>? {
            return null
        }

        override fun setListenerPosition(position: Vector3fc, lookAt: Vector3fc, up: Vector3fc) {}

        override fun playSoundEffect(soundEffect: String, mode: Mode, position: Vector3dc?, pitch: Float, gain: Float,
                                     attStart: Float, attEnd: Float): SoundSource {
            // TODO Auto-generated method stub
            return DummySoundSource()
        }

    }

    class NullParticlesManager : ParticlesManager {

        override fun spawnParticleAtPosition(particleTypeName: String, location: Vector3dc) {
            // TODO Auto-generated method stub

        }

        override fun spawnParticleAtPositionWithVelocity(particleTypeName: String, location: Vector3dc,
                                                         velocity: Vector3dc) {
            // TODO Auto-generated method stub

        }
    }

    class NullDecalsManager : DecalsManager {

        override fun add(vector3dc: Vector3dc, vector3dc1: Vector3dc, vector3dc2: Vector3dc, s: String) {

        }
    }

    override fun spawnPlayer(player: Player) {
        throw UnsupportedOperationException("spawnPlayer")
    }

    override fun getPlayerByName(playerName: String): Player? {
        throw UnsupportedOperationException("getPlayers")
    }
}
