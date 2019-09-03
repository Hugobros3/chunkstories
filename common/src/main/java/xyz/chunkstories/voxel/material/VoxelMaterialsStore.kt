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
import xyz.chunkstories.voxel.VoxelsStore
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.content.eat
import xyz.chunkstories.content.extractProperties
import java.util.*

class VoxelMaterialsStore(private val voxels: VoxelsStore) : Content.Voxels.VoxelMaterials {
    override val parent = voxels.parent

    internal var materials: MutableMap<String, VoxelMaterial> = HashMap()

    override lateinit var defaultMaterial: VoxelMaterial

    fun reload() {
        materials.clear()
        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading voxel material definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["materials"].asDict ?: throw Exception("This json doesn't contain an 'materials' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val voxelMaterial = VoxelMaterial(this, name, properties)
                materials[name] = voxelMaterial

                if (voxelMaterial.name == "default")
                    defaultMaterial = voxelMaterial

                logger.debug("Loaded voxel material $voxelMaterial")
            }
        }

        for (asset in voxels.parent.modsManager.allAssets.filter { it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getVoxelMaterial(materialName: String): VoxelMaterial? = materials[materialName]

    override val all: Collection<VoxelMaterial>
        get() {
            return materials.values
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.materials")
    }

    override val logger: Logger
        get() = Companion.logger
}