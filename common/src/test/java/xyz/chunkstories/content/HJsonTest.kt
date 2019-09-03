package xyz.chunkstories.content

import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.junit.Test
import xyz.chunkstories.api.content.json.Json

class HJsonTest {
    @Test
    fun testHjsonTranslation() {
        val json = JsonValue.readHjson(text).eat()
        println(json)
        //val map: LinkedTreeMap<Any?, Any?> = gson.fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<Any?, Any?>
    }


    val text = """
        {
    lootTables: {
        trashButMaybeGood: {
            description: "Has a large chance of spawning garbage, but might spawn good stuff too"
            // pick_one means one of the entries will be chosen, based on the weights
            type: pick_one
            ow: false
            ow2: null
            entries: [
                {
                    // The most likely option to get picked is the "trash" group
                    type: pick_one
                    weight: 0.8
                    entries: [
                        {
                            item: coal
                            amount: 1
                            weight: 0.8
                        },
                        {
                            item: stick
                            amount_max: 5
                            weight: 1.0
                        },
                        {
                            item: cobble
                            amount_max: 7
                            weight: 0.8
                        },
                        {
                            item: wood_pickaxe
                            weight: 0.4
                        }
                    ]
                },
                {
                    // It's also possible, but unlikely, to receive nothing.
                    type: nothing
                    weight: 0.1
                },
                {
                    // Equally unlikely is to get treasure
                    type: pick_one
                    weight: 0.1
                    entries: [
                        {
                            item: iron_bar
                            weight: 0.6
                        },
                        {
                            item: gold_bar
                            weight: 0.2
                        },
                        {
                            item: diamond
                            weight: 0.1
                        }
                    ]
                }
            ]
        },
        starterKit: {
            description: "A starter kit to give to new players in easy mode"
            // all_of means all the entries will be evaluated
            type: all_of
            entries: [
                {
                    item: wood_sword
                },
                {
                    item: wood_pickaxe
                },
                {
                    item: wood_axe
                },
                {
                    item: wood_shovel
                },
                {
                    item: bread
                    amount: 8
                },
                {
                    item: torch
                    amount: 16
                }
            ]
        }
    }
}
    """.trimIndent()
}