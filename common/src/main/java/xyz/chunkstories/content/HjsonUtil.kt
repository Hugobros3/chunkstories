package xyz.chunkstories.content

import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import xyz.chunkstories.api.content.json.Json

@Deprecated("lol this sucks")
fun LinkedTreeMap<String, *>.extractProperties(): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    fun extract(prefix: String, subtree: LinkedTreeMap<String, *>) {
        for(entry in subtree.entries) {
            val propertyName = prefix + entry.key
            val propertyValue = entry.value
            when (propertyValue) {
                is LinkedTreeMap<*, *> -> {
                    map[propertyName] = "exists"
                    extract("$propertyName.", propertyValue as LinkedTreeMap<String, *>)
                }
                is String -> map[propertyName] = propertyValue
                is Any -> map[propertyName] = propertyValue.toString()
            }
        }
    }
    extract("", this)
    return map
}

fun JsonValue.eat() : Json =
        when {
            this is JsonObject -> {
                Json.Dict(this.names().map {
                    Pair(it, this[it].eat())
                }.toMap())
            }
            this is JsonArray -> {
                Json.Array(this.values().map { it.eat() })
            }
            this.isNull -> Json.Value.Null
            this.isBoolean -> Json.Value.Bool(this.asBoolean())
            this.isNumber -> Json.Value.Number(this.asDouble())
            this.isString -> Json.Value.Text(this.asString())
            else -> throw Exception("Well whatever this is, it isn't json: $this")
        }