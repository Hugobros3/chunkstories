//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.item

import ItemDefinitionsLexer
import ItemDefinitionsParser
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.Content.ItemsDefinitions
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.content.GameContentStore
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
            map.put(prefix + it.Name().text, it.value().getValue())
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }
}

private fun ItemDefinitionsParser.ValueContext.getValue(): String {
    if (this.Text() != null)
        return this.Text().text.substring(1, this.Text().text.length - 1)
    else return this.text
}
