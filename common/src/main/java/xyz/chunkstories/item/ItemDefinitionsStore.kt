//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.item

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.Content.ItemsDefinitions
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.extractProperties
import java.util.*

class ItemDefinitionsStore(override val parent: GameContentStore) : ItemsDefinitions {
    var itemDefinitions: MutableMap<String, ItemDefinition> = HashMap()

    private val modsManager: ModsManager

    override val logger = LoggerFactory.getLogger("content.items")

    init {
        this.modsManager = parent.modsManager
    }

    fun reload() {
        itemDefinitions.clear()

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading items definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).toString()
            val map: LinkedTreeMap<Any?, Any?> = gson.fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<Any?, Any?>

            val materialsTreeMap = map["items"] as LinkedTreeMap<*, *>

            for (definition in materialsTreeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                val itemDefinition = ItemDefinition(this, name, properties)
                itemDefinitions.put(name, itemDefinition)

                logger.debug("Loaded item definition $itemDefinition")
            }
        }

        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("items/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    internal fun addVoxelItems() {
        // Include definitions from the voxels variants
        for(voxel in parent.voxels.all) {
            for(variantDefinition in voxel.variants) {
                itemDefinitions[variantDefinition.name] = variantDefinition
            }
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