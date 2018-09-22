//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.entity

import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import DefinitionsLexer
import DefinitionsParser
import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.content.Content.EntityDefinitions
import io.xol.chunkstories.api.entity.EntityDefinition
import io.xol.chunkstories.content.GameContentStore
import io.xol.chunkstories.util.format.toMap
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

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

        fun readEntitiesDefinitions(a: Asset) {
            logger().debug("Reading entities definitions in : $a")

            val text = a.reader().use { it.readText() }
            val parser = DefinitionsParser(CommonTokenStream(DefinitionsLexer(ANTLRInputStream(text))))

            for(definition in parser.entitiesDefinitions().entitiesDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val entityDefinition = EntityDefinition(this, name, properties)
                entityDefinitions.put(name, entityDefinition)

                logger().debug("Loaded entity definition $entityDefinition")
            }
        }

        val i = content.modsManager().getAllAssetsByExtension("entities")
        while (i.hasNext()) {
            val f = i.next()
            readEntitiesDefinitions(f)
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
}
