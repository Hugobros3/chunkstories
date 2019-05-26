//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.voxel

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.extractProperties
import xyz.chunkstories.voxel.material.VoxelMaterialsStore
import java.util.*

class VoxelsStore(private val content: GameContentStore) : Content.Voxels {

    private val materials: VoxelMaterialsStore = VoxelMaterialsStore(this)
    private val textures: ReloadableVoxelTextures

    var voxelsByName: MutableMap<String, Voxel> = HashMap()
    private lateinit var air: Voxel

    init {
        val backend = (content.context as? Client)?.graphics?.backend

        textures = when (backend) {
            is VoxelTexturesSupport ->
                backend.createVoxelTextures(this)
            else ->
                DummyVoxelTextures(this)
        }
    }

    override fun materials(): VoxelMaterialsStore {
        return materials
    }

    override fun textures(): Content.Voxels.VoxelTextures {
        return textures
    }

    fun reload() {
        this.materials.reload()
        this.textures.reload()

        air = Voxel(VoxelDefinition(this, "air", mapOf(
                "solid" to "false",
                "opaque" to "false"
        )))
        this.reloadVoxelTypes()

        parent().items().addVoxelItems()
    }

    private fun reloadVoxelTypes() {
        voxelsByName.clear()
        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger().debug("Reading blocks definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java)

            val blocksTreeMap = map["blocks"] as LinkedTreeMap<*, *>

            for (definition in blocksTreeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                val voxelDefinition = VoxelDefinition(this, name, properties)
                val voxel: Voxel = voxelDefinition.create()
                voxelsByName[name] = voxel

                logger.debug("Loaded $voxelDefinition from $a, created $voxel")
            }
        }

        air = Voxel(VoxelDefinition(this, "air", mapOf(
                "solid" to "false",
                "opaque" to "false"
        )))
        voxelsByName["air"] = air

        for (asset in content.modsManager().allAssets.filter { it.name.startsWith("voxels/") && !it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getVoxel(voxelName: String): Voxel? {
        return voxelsByName[voxelName]
    }

    override fun all(): Collection<Voxel> {
        return voxelsByName.values
    }

    override fun parent(): GameContentStore {
        return content
    }

    override fun logger(): Logger {
        return logger
    }

    override fun air(): Voxel {
        return air
    }

    companion object {
        private val logger = LoggerFactory.getLogger("content.voxels")
    }
}
