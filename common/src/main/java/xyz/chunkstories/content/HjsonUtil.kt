package xyz.chunkstories.content

import com.google.gson.internal.LinkedTreeMap

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