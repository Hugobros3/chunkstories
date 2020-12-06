package xyz.chunkstories.crafting

import org.hjson.JsonValue
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.crafting.PatternedRecipe
import xyz.chunkstories.api.crafting.Recipe
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat

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

    override fun reload() {
        all.clear()

        fun loadRecipes(asset: Asset) {
            logger.debug("Reading recipes in :$asset")

            //val gson = Gson()
            val json = JsonValue.readHjson(asset.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val array = json["recipes"].asArray ?: throw Exception("This json doesn't contain an 'recipes' array")

            for (recipeJson in array.elements) {
                if(recipeJson !is Json.Dict)
                    throw Exception("Recipes should be dicts! ($recipeJson)")

                try {
                    val resolvedResult = when(val result = recipeJson["result"]) {
                        is Json.Value.Text -> Pair(store.items.getItemDefinition(result.text)!!, 1)
                        is Json.Array -> Pair(store.items.getItemDefinition(result.elements[0].asString!!)!!, result.elements.getOrNull(1).asInt ?: 1)
                        else -> throw Exception("What to do with $result")
                    }

                    val ingredients = recipeJson["ingredients"].asDict ?: throw Exception("No ingredients!")
                    val ingredientsMap = ingredients.elements.entries.map { Pair(it.key, store.items.getItemDefinition(it.value.asString!!)!!) }.toMap()

                    val pattern = recipeJson["pattern"].asString
                    if (pattern != null) {

                        val patternLines = pattern.lines()
                        val patternHeight = patternLines.size
                        val patternWidth = patternLines.map { it.length }.max()!!
                        val resolvedPattern = Array(patternHeight) { y ->
                            Array(patternWidth) { x -> patternLines[y].toCharArray().getOrNull(x)?.let { ingredientsMap[it.toString()] } }
                        }
                        val recipe = PatternedRecipe(resolvedPattern, resolvedResult)
                        all += recipe
                        logger.info("Successfully loaded recipe $recipe")
                    } else {
                        TODO("Implement recipes with no pattern and a list of ingredients instead")
                    }
                } catch (e: Exception) {
                    Recipe.logger.error("Failed to load recipe $recipeJson $e")
                }
            }
        }

        for (asset in store.modsManager.allAssets.filter { it.name.startsWith("recipes/") && it.name.endsWith(".hjson") }) {
            loadRecipes(asset)
        }
    }
}