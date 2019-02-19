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

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val materials2 = map["materials"] as LinkedTreeMap<*, *>

            for (definition in materials2.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

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
}


private fun LinkedTreeMap<String, *>.extractProperties(): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    fun extract(prefix: String, subtree: LinkedTreeMap<String, *>) {
        for(entry in subtree.entries) {
            val propertyName = prefix + entry.key
            val propertyValue = entry.value
            if(propertyValue is String) {
                map[propertyName] = propertyValue
            } else if(propertyValue is LinkedTreeMap<*, *>) {
                map[propertyName] = "exists"
                extract("$propertyName.", propertyValue as LinkedTreeMap<String, *>)
            }
        }
    }
    extract("", this)
    return map
}