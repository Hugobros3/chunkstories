package io.xol.chunkstories.util.format

public fun DefinitionsParser.PropertiesContext.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()

    this.extractIn(map, "")

    return map
}

public fun DefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
    this.property().forEach {
        map.put(prefix + it.Name().text, it.value().text)
    }

    this.compoundProperty().forEach {
        map.put(prefix + it.Name().text, "exists")
        it.properties().extractIn(map, prefix + it.Name().text + ".")
    }
}
