package xyz.chunkstories.loot

import org.hjson.JsonValue
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.loot.LootTable
import xyz.chunkstories.api.loot.makeLootTableFromJson
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat

class LootTablesStore(val store: GameContentStore) : Content.LootTables {
    val logger = LoggerFactory.getLogger("content.loot")

    override val all = mutableMapOf<String, LootTable>()

    override fun reload() {
        all.clear()

        fun loadLootTables(asset: Asset) {
            logger.debug("Reading loot tables in :$asset")

            val json = JsonValue.readHjson(asset.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["lootTables"].asDict ?: throw Exception("This json doesn't contain an 'lootTables' dict")

            for (element in dict.elements) {
                val name = element.key
                try {
                    val table = makeLootTableFromJson(element.value, store, null)
                    all[name] = table
                    logger.info("Successfully loaded loot table $name")
                } catch(e: Exception) {
                    logger.error("Failed to load loot table $name: $e")
                }
            }
        }

        for (asset in store.modsManager.allAssets.filter { it.name.startsWith("loot_tables/") && it.name.endsWith(".hjson") }) {
            loadLootTables(asset)
        }
    }

}