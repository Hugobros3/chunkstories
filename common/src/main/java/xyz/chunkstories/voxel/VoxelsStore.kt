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
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import xyz.chunkstories.content.extractProperties
import xyz.chunkstories.voxel.material.VoxelMaterialsStore
import java.util.*

class VoxelsStore(override val parent: GameContentStore) : Content.Voxels {

    override val materials: VoxelMaterialsStore = VoxelMaterialsStore(this)
    override val textures: ReloadableVoxelTextures

    var voxelsByName: MutableMap<String, Voxel> = HashMap()
    override lateinit var air: Voxel

    init {
        val backend = (parent.context as? Client)?.graphics?.backend

        textures = when (backend) {
            is VoxelTexturesSupport ->
                backend.createVoxelTextures(this)
            else ->
                DummyVoxelTextures(this)
        }
    }

    fun reload() {
        this.materials.reload()
        this.textures.reload()

        air = Voxel(VoxelDefinition(this, "air", Json.Dict(mapOf(
                "solid" to Json.Value.Bool(false),
                "opaque" to Json.Value.Bool(false)
        ))))
        this.reloadVoxelTypes()

        parent.items.addVoxelItems()
    }

    private fun reloadVoxelTypes() {
        voxelsByName.clear()
        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading blocks definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["blocks"].asDict ?: throw Exception("This json doesn't contain an 'blocks' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val voxelDefinition = VoxelDefinition(this, name, properties)
                val voxel: Voxel = voxelDefinition.voxel
                voxelsByName[name] = voxel

                logger.debug("Loaded $voxelDefinition from $a, created $voxel")
            }
        }

        air = Voxel(VoxelDefinition(this, "air", Json.Dict(mapOf(
                "solid" to Json.Value.Bool(false),
                "opaque" to Json.Value.Bool(false)
        ))))
        voxelsByName["air"] = air

        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("voxels/") && !it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getVoxel(voxelName: String): Voxel? {
        return voxelsByName[voxelName]
    }

    override val all: Collection<Voxel>
        get() {
            return voxelsByName.values
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.voxels")
    }

    override val logger: Logger
        get() = Companion.logger
}
