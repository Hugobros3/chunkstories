//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.item

import DefinitionsLexer
import DefinitionsParser
import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.content.Content.ItemsDefinitions
import io.xol.chunkstories.api.content.mods.ModsManager
import io.xol.chunkstories.api.item.ItemDefinition
import io.xol.chunkstories.content.GameContentStore
import io.xol.chunkstories.util.format.toMap
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ItemDefinitionsStore(gameContentStore: GameContentStore) : ItemsDefinitions {
    var itemDefinitions: MutableMap<String, ItemDefinition> = HashMap()

    private val content: Content
    private val modsManager: ModsManager

    private val logger = LoggerFactory.getLogger("content.items")

    override fun logger(): Logger {
        return logger
    }

    init {
        this.content = gameContentStore
        this.modsManager = gameContentStore.modsManager()
    }

    fun reload() {
        itemDefinitions.clear()

        fun readItemsDefinitions(a: Asset) {
            val text = a.reader().use { it.readText() }
            val parser = DefinitionsParser(CommonTokenStream(DefinitionsLexer(ANTLRInputStream(text))))

            for (definition in parser.itemDefinitions().itemDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val itemDefinition = ItemDefinition(this, name, properties)
                itemDefinitions.put(name, itemDefinition)

                logger().debug("Loaded item definition $itemDefinition")
            }
        }

        val i = modsManager.getAllAssetsByExtension("items")
        while (i.hasNext()) {
            val f = i.next()
            logger().debug("Reading items definitions in : $f")
            readItemsDefinitions(f)
        }
    }

    override fun getItemDefinition(itemName: String)
            : ItemDefinition? {
        return if (itemDefinitions.containsKey(itemName)) itemDefinitions[itemName] else null
    }

    override fun all(): Iterator<ItemDefinition> {
        return itemDefinitions.values.iterator()
    }

    override fun parent(): Content {
        return content
    }
}
