//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.Engine

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.ContentTranslator
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.workers.Tasks
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.api.world.World
import xyz.chunkstories.content.mods.ModsManagerImplementation

/** Dummy GameContext for testing purposes  */
class TestGameContext(mods: String) : GameInstance {

    override val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    override val engine: EngineImplemI
        get() = TODO("Not yet implemented")
    override val world: World
        get() = TODO("Not yet implemented")
    override val contentTranslator: ContentTranslator
        get() = TODO("Not yet implemented")

    override val pluginManager: PluginManager
        get() = throw UnsupportedOperationException()

    override val content: GameContentStore

    init {

        val coreContentLocation = System.getProperty("coreContentLocation", "../../chunkstories-core/res/")

        content = GameContentStore(engine, File(coreContentLocation), emptyList())
        content.reload()
    }
}

class DummyEngine : EngineImplemI {
    override val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    override val tasks: Tasks
        get() = throw UnsupportedOperationException()

    override val modsManager: ModsManagerImplementation
        get() = TODO("Not yet implemented")
}