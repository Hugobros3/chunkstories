//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.generator

import DefinitionsLexer
import DefinitionsParser
import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.content.mods.ModsManager
import io.xol.chunkstories.api.world.generator.BlankWorldGenerator
import io.xol.chunkstories.api.world.generator.WorldGeneratorDefinition
import io.xol.chunkstories.content.GameContentStore
import io.xol.chunkstories.util.format.toMap
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class WorldGeneratorsStore(private val store: GameContentStore) : Content.WorldGenerators {
    private val modsManager: ModsManager = store.modsManager()

    var generators: MutableMap<String, WorldGeneratorDefinition> = HashMap()

    /** Vanilla blank (void) world generator  */
    internal var blank = WorldGeneratorDefinition(this, "blank", mapOf( "class" to BlankWorldGenerator::class.java.canonicalName))

    override fun logger(): Logger {
        return logger
    }

    fun reload() {
        // Loads all generators
        generators.clear()

        fun loadWorldGeneratorsFile(a: Asset) {
            logger().debug("Reading WorldGenerators declarations in : $a")

            val text = a.reader().use { it.readText() }
            val parser = DefinitionsParser(CommonTokenStream(DefinitionsLexer(ANTLRInputStream(text))))

            for(definition in parser.worldGeneratorDefinitions().worldGeneratorDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val generatorDefinition = WorldGeneratorDefinition(this, name, properties)
                generators.put(name, generatorDefinition)
                logger.debug("Loaded $generatorDefinition from $a")
            }
        }

        val i = modsManager.getAllAssetsByExtension("generators")
        while (i.hasNext()) {
            val f = i.next()
            loadWorldGeneratorsFile(f)
        }
    }

    override fun getWorldGenerator(name: String): WorldGeneratorDefinition {
        val generator = generators[name]
        if (generator != null)
            return generator

        logger().warn("Couldn't find generator \"$name\"; Providing BlankWorldGenerator instead.")
        return blank
    }

    override fun all(): Iterator<WorldGeneratorDefinition> {
        return generators.values.iterator()
    }

    override fun parent(): Content {
        return store
    }

    companion object {
        private val logger = LoggerFactory.getLogger("content.generators")
    }
}

