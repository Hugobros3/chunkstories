//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.generator

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.world.generator.BlankWorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import xyz.chunkstories.content.GameContentStore
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.content.eat
import xyz.chunkstories.content.extractProperties
import java.util.*

class WorldGeneratorsStore(override val parent: GameContentStore) : Content.WorldGenerators {
    var generators: MutableMap<String, WorldGeneratorDefinition> = HashMap()

    /** Vanilla blank (void) world generator  */
    internal var blank = WorldGeneratorDefinition(this, "blank", Json.Dict(mapOf( "class" to Json.Value.Text(BlankWorldGenerator::class.java.canonicalName))))

    fun reload() {
        // Loads all generators
        generators.clear()

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading generators definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["generators"].asDict ?: throw Exception("This json doesn't contain an 'generators' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val generatorDefinition = WorldGeneratorDefinition(this, name, properties)
                generators[name] = generatorDefinition
                logger.debug("Loaded $generatorDefinition from $a")
            }
        }

        for(asset in parent.modsManager.allAssets.filter { it.name.startsWith("generators/")  && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getWorldGenerator(name: String): WorldGeneratorDefinition {
        val generator = generators[name]
        if (generator != null)
            return generator

        logger.warn("Couldn't find generator \"$name\"; Providing BlankWorldGenerator instead.")
        return blank
    }

    override val all: Collection<WorldGeneratorDefinition>
        get() {
            return generators.values
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.generators")
    }

    override val logger: Logger
        get() = Companion.logger
}

