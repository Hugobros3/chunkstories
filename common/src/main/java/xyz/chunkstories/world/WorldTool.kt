//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import java.io.File

import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.world.WorldInfo
import org.joml.Vector3dc
import org.joml.Vector3fc

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.util.IterableIterator
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.sound.source.DummySoundSource
import xyz.chunkstories.world.io.IOTasks

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
            worldInfoFile.writeText(xyz.chunkstories.world.serializeWorldInfo(worldInfo, true))
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
        override fun <T : ParticleType.Particle> spawnParticle(typeName: String, init: T.() -> Unit) {
        }

        override fun <T : ParticleType.Particle> spawnParticle(type: ParticleTypeDefinition, init: T.() -> Unit) {
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
