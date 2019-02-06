//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.voxel.material

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.voxel.materials.VoxelMaterial
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.voxel.VoxelsStore
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class VoxelMaterialsStore(private val voxels: VoxelsStore) : Content.Voxels.VoxelMaterials {
    private val store: GameContentStore

    internal var materials: MutableMap<String, VoxelMaterial> = HashMap()

    override lateinit var defaultMaterial: VoxelMaterial

    override fun logger(): Logger {
        return logger
    }

    init {
        this.store = voxels.parent()
    }

    fun reload() {
        materials.clear()
        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger().debug("Reading voxel material definitions in : $a")

            //val text = a.reader().use { it.readText() }
            //val parser = MaterialDefinitionsParser(CommonTokenStream(MaterialDefinitionsLexer(ANTLRInputStream(text))))

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val materials2 = map["materials"] as LinkedTreeMap<*, *>

            for (definition in materials2.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<*, *>).entries.toMap().toMutableMap() as MutableMap<String, String>

                properties["name"] = name

                val voxelMaterial = VoxelMaterial(this, name, properties)
                materials.put(name, voxelMaterial)

                if (voxelMaterial.name == "default")
                    defaultMaterial = voxelMaterial

                logger.debug("Loaded voxel material $voxelMaterial")
            }
        }

        for (asset in store.modsManager().allAssets.filter { it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getVoxelMaterial(name: String): VoxelMaterial {
        val material = materials[name]
        return material ?: getVoxelMaterial("undefined")

    }

    override fun all(): Iterator<VoxelMaterial> {
        return materials.values.iterator()
    }

    override fun parent(): Content {
        return store
    }

    companion object {
        private val logger = LoggerFactory.getLogger("content.materials")
    }

    public fun MaterialDefinitionsParser.PropertiesContext?.toMap(): Map<String, String> {
        if (this == null)
            return emptyMap()

        val map = mutableMapOf<String, String>()

        this.extractIn(map, "")

        return map
    }

    public fun MaterialDefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
        this.property().forEach {
            map.put(prefix + it.Name().text, it.value().getValue())
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }
}

private fun Collection<MutableMap.MutableEntry<*, *>>.toMap() = map { Pair(it.key, it.value) }.toMap()

//TODO apply that to all
private fun MaterialDefinitionsParser.ValueContext.getValue(): String {
    if (this.Text() != null)
        return this.Text().text.substring(1, this.Text().text.length - 1)
    else return this.text
}
