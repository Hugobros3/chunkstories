//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.item

import ItemDefinitionsLexer
import ItemDefinitionsParser
import io.xol.chunkstories.api.content.Asset
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.content.Content.ItemsDefinitions
import io.xol.chunkstories.api.content.mods.ModsManager
import io.xol.chunkstories.api.item.ItemDefinition
import io.xol.chunkstories.content.GameContentStore
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

        fun readDefinitions(a: Asset) {
            logger().debug("Reading items definitions in : $a")

            val text = a.reader().use { it.readText() }
            val parser = ItemDefinitionsParser(CommonTokenStream(ItemDefinitionsLexer(ANTLRInputStream(text))))

            for (definition in parser.itemDefinitions().itemDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val itemDefinition = ItemDefinition(this, name, properties)
                itemDefinitions.put(name, itemDefinition)

                logger().debug("Loaded item definition $itemDefinition")
            }
        }

        for(asset in content.modsManager().allAssets.filter { it.name.startsWith("items/") && it.name.endsWith(".def") }) {
            readDefinitions(asset)
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

    fun ItemDefinitionsParser.PropertiesContext?.toMap(): Map<String, String> {
        if(this == null)
            return emptyMap()

        val map = mutableMapOf<String, String>()

        this.extractIn(map, "")

        return map
    }

    fun ItemDefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
        this.property().forEach {
            map.put(prefix + it.Name().text, it.value().text)
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }
}
