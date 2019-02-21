//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.workers.Tasks

/** Dummy GameContext for testing purposes  */
class TestGameContext(mods: String) : GameContext {

    private val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    override val content: GameContentStore

    override val pluginManager: PluginManager
        get() = throw UnsupportedOperationException()

    override val tasks: Tasks
        get() = throw UnsupportedOperationException()

    init {

        val coreContentLocation = System.getProperty("coreContentLocation", "../chunkstories-core/res/")

        content = GameContentStore(this, File(coreContentLocation), mods)
        content.reload()
    }

    override fun print(message: String) {
        logger.info(message)
    }

    override fun logger(): Logger {
        return logger
    }

}
