//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity

import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import EntityDefinitionsLexer
import EntityDefinitionsParser
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.Content.EntityDefinitions
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.content.GameContentStore
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.hjson.JsonValue
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.content.extractProperties

class EntityDefinitionsStore(content: GameContentStore) : EntityDefinitions {
    private val content: Content

    private val entityDefinitions = HashMap<String, EntityDefinition>()

    override fun logger(): Logger {
        return logger
    }

    init {
        this.content = content
    }

    fun reload() {
        entityDefinitions.clear()

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger().debug("Reading entities definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val materialsTreeMap = map["entities"] as LinkedTreeMap<*, *>

            for (definition in materialsTreeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                val entityDefinition = EntityDefinition(this, name, properties)
                entityDefinitions.put(name, entityDefinition)

                logger().debug("Loaded entity definition $entityDefinition")
            }
        }

        for(asset in content.modsManager().allAssets.filter { it.name.startsWith("entities/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getEntityDefinition(TraitName: String): EntityDefinition? {
        return entityDefinitions[TraitName]
    }

    override fun all(): Iterator<EntityDefinition> {
        return this.entityDefinitions.values.iterator()
    }

    override fun parent(): Content {
        return content
    }

    companion object {

        private val logger = LoggerFactory.getLogger("content.entities")
    }

    public fun EntityDefinitionsParser.PropertiesContext?.toMap(): Map<String, String> {
        if(this == null)
            return emptyMap()

        val map = mutableMapOf<String, String>()

        this.extractIn(map, "")

        return map
    }

    public fun EntityDefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
        this.property().forEach {
            map.put(prefix + it.Name().text, it.value().getValue())
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }

    private fun EntityDefinitionsParser.ValueContext.getValue(): String {
        if (this.Text() != null)
            return this.Text().text.substring(1, this.Text().text.length - 1)
        else return this.text
    }
}
