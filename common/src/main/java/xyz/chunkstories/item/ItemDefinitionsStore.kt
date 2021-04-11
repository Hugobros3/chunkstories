//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.item

import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content.ItemsDefinitions
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import java.util.*

class ItemDefinitionsStore(override val parent: GameContentStore) : ItemsDefinitions {
    var itemDefinitions: MutableMap<String, ItemDefinition> = HashMap()

    private val modsManager: ModsManager = parent.modsManager

    override val logger: Logger = LoggerFactory.getLogger("content.items")

    fun reload() {
        itemDefinitions.clear()

        fun readDefinitions(asset: Asset) {
            logger.debug("Reading items definitions in :$asset")

            val json = JsonValue.readHjson(asset.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["items"].asDict ?: throw Exception("This json doesn't contain an 'items' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val itemDefinition = ItemDefinition(this, name, properties)
                itemDefinitions.put(name, itemDefinition)

                logger.debug("Loaded item definition $itemDefinition")
            }
        }

        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("items/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getItemDefinition(itemName: String)
            : ItemDefinition? {
        return if (itemDefinitions.containsKey(itemName)) itemDefinitions[itemName] else null
    }

    override val all: Collection<ItemDefinition>
        get() {
            return itemDefinitions.values
        }
}