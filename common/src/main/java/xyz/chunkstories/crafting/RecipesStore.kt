package xyz.chunkstories.crafting

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonValue
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.crafting.PatternedRecipe
import xyz.chunkstories.api.crafting.Recipe
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.content.GameContentStore

class RecipesStore(val store: GameContentStore) : Content.Recipes {

    override val all = mutableListOf<Recipe>()

    val logger = LoggerFactory.getLogger("content.items")

    override fun getRecipeForInventorySlots(craftingAreaSlots: Array<Array<InventorySlot.FakeSlot>>): Recipe? {
        for(recipe in all) {
            if(recipe.canCraftUsing(craftingAreaSlots))
                return recipe
        }
        return null
    }

    override fun reloadAll() {
        all.clear()

        fun loadRecipes(asset: Asset) {
            val gson = Gson()
            val json = JsonValue.readHjson(asset.reader()).toString()
            val map: LinkedTreeMap<Any?, Any?> = gson.fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<Any?, Any?>


            for (recipe in map["recipes"] as ArrayList<LinkedTreeMap<Any?, Any?>>) {
                try {
                    val result = recipe["result"] as? String ?: throw Exception("No result!")
                    val resolvedResult = store.items.getItemDefinition(result)!!

                    val pattern = recipe["pattern"] as? String
                    if (pattern != null) {
                        val ingredients = recipe["ingredients"] as? LinkedTreeMap<String, String> ?: throw Exception("No ingredients!")
                        val ingredientsMap = ingredients.entries.map { Pair(it.key!!, store.items.getItemDefinition(it.value)) }.toMap()

                        val patternLines = pattern.lines()
                        val patternHeight = patternLines.size
                        val patternWidth = patternLines.map { it.length }.max()!!
                        val resolvedPattern = Array(patternHeight) { y ->
                            Array(patternWidth) { x -> patternLines[y].toCharArray().getOrNull(x)?.let { ingredientsMap[it.toString()] } }
                        }//pattern.lines().map { it.map { ingredientsMap[it.toString()] }.toTypedArray() }.toTypedArray()
                        val recipe = PatternedRecipe(resolvedPattern, resolvedResult)
                        all += recipe
                        logger.info("Successfully loaded recipe $recipe")
                    }
                } catch (e: Exception) {
                    Recipe.logger.error("Failed to load recipe $recipe")
                }
            }
        }

        for (asset in store.modsManager.allAssets.filter { it.name.startsWith("recipes/") && it.name.endsWith(".hjson") }) {
            loadRecipes(asset)
        }
    }
}