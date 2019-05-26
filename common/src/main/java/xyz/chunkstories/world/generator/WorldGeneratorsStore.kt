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
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.world.generator.BlankWorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import xyz.chunkstories.content.GameContentStore
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.content.extractProperties
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

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger().debug("Reading generators definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val treeMap = map["generators"] as LinkedTreeMap<*, *>

            for (definition in treeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                val generatorDefinition = WorldGeneratorDefinition(this, name, properties)
                generators.put(name, generatorDefinition)
                logger.debug("Loaded $generatorDefinition from $a")
            }
        }

        for(asset in store.modsManager().allAssets.filter { it.name.startsWith("generators/")  && it.name.endsWith(".hjson") }) {
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

    override fun all(): Collection<WorldGeneratorDefinition> {
        return generators.values
    }

    override fun parent(): Content {
        return store
    }

    companion object {
        private val logger = LoggerFactory.getLogger("content.generators")
    }
}

