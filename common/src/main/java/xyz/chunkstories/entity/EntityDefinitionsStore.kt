//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity

import com.google.gson.Gson
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content.EntityDefinitions
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import java.util.*

class EntityDefinitionsStore(override val parent: GameContentStore) : EntityDefinitions {
    private val entityDefinitions = HashMap<String, EntityDefinition>()

    fun reload() {
        entityDefinitions.clear()

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading entities definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["entities"].asDict ?: throw Exception("This json doesn't contain an 'entities' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val entityDefinition = EntityDefinition(this, name, properties)
                entityDefinitions[name] = entityDefinition

                logger.debug("Loaded entity definition $entityDefinition")
            }
        }

        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("entities/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getEntityDefinition(TraitName: String): EntityDefinition? {
        return entityDefinitions[TraitName]
    }

    override val all: Collection<EntityDefinition>
        get() {
            return this.entityDefinitions.values
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.entities")
    }

    override val logger: Logger
        get() = Companion.logger
}
