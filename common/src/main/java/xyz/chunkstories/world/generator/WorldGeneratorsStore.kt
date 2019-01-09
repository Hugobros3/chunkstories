//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.generator

import WorldGeneratorDefinitionsLexer
import WorldGeneratorDefinitionsParser
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.world.generator.BlankWorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import xyz.chunkstories.content.GameContentStore
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

        fun readDefinitions(a: Asset) {
            logger().debug("Reading wrld generators definitions in : $a")

            val text = a.reader().use { it.readText() }
            val parser = WorldGeneratorDefinitionsParser(CommonTokenStream(WorldGeneratorDefinitionsLexer(ANTLRInputStream(text))))

            for(definition in parser.worldGeneratorDefinitions().worldGeneratorDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val generatorDefinition = WorldGeneratorDefinition(this, name, properties)
                generators.put(name, generatorDefinition)
                logger.debug("Loaded $generatorDefinition from $a")
            }
        }

        for(asset in store.modsManager().allAssets.filter { it.name.startsWith("generators/")  && it.name.endsWith(".def") }) {
            readDefinitions(asset)
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

    public fun WorldGeneratorDefinitionsParser.PropertiesContext?.toMap(): Map<String, String> {
        if(this == null)
            return emptyMap()

        val map = mutableMapOf<String, String>()

        this.extractIn(map, "")

        return map
    }

    public fun WorldGeneratorDefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
        this.property().forEach {
            map.put(prefix + it.Name().text, it.value().text)
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }
}

