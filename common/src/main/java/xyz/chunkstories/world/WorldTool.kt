//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import java.io.File

import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import org.joml.Vector3dc

import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.sound.SoundManager
import xyz.chunkstories.api.world.World
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.content.translator.InitialContentTranslator
import xyz.chunkstories.world.io.IOTasks

class WorldTool constructor(override val gameInstance: Host,
                            properties: World.Properties,
                            internalData: WorldInternalData,
                            contentTranslator: AbstractContentTranslator,
                            folder: File,
                            val immediateIO: Boolean) :
        WorldMasterImplementation(gameInstance, properties, internalData, contentTranslator, folder) {

    var isLightningEnabled = true
    var isGenerationEnabled = true

    override val ioHandler: IOTasks = IOTasks(this)

    override val soundManager: SoundManager
        get() = throw UnsupportedOperationException()

    override val particlesManager: ParticlesManager
        get() = nullParticlesManager

    private var nullParticlesManager = NoOpParticlesManager()

    override val decalsManager: DecalsManager
        get() = nullDecalsManager

    private var nullDecalsManager = NoOpDecalsManager()

    override val players: Sequence<Player>
        get() = throw UnsupportedOperationException("getPlayers")

    override var sky: World.Sky
        get() = throw UnsupportedOperationException()
        set(value) = throw UnsupportedOperationException()

    init {
        ioHandler.start()
    }

    companion object {
        fun Host.createWorld(folder: File, properties: World.Properties) : WorldTool {
            if(folder.exists())
                throw Exception("The folder $folder already exists !")

            logger.debug("Creating new world")
            folder.mkdirs()
            val worldInfoFile = File(folder.path + "/" + WorldImplementation.worldPropertiesFilename)
            worldInfoFile.writeText(serializeWorldInfo(properties, true))
            logger.debug("Created directory & wrote ${WorldImplementation.worldPropertiesFilename} ; now entering world")

            val contentTranslator = InitialContentTranslator(content as GameContentStore)
            contentTranslator.save(File(folder.path + "/content_mappings.dat"))

            val internalData = tryLoadWorldInternalData(folder)

            return WorldTool(this, properties, internalData, contentTranslator, folder, false)
        }
    }
}
