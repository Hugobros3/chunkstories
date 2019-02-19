//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.voxel

import VoxelDefinitionsParser
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.voxel.material.VoxelMaterialsStore
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.content.extractProperties
import java.util.*

class VoxelsStore(private val content: GameContentStore) : Content.Voxels {

    private val materials: VoxelMaterialsStore = VoxelMaterialsStore(this)
    private val textures: VoxelTexturesStoreAndAtlaser = VoxelTexturesStoreAndAtlaser(this)

    var voxelsByName: MutableMap<String, Voxel> = HashMap()
    private lateinit var air: Voxel

    override fun materials(): VoxelMaterialsStore {
        return materials
    }

    override fun textures(): VoxelTexturesStoreAndAtlaser {
        return textures
    }

    fun reload() {
        this.materials.reload()
        this.textures.buildTextureAtlas()

        air = Voxel(xyz.chunkstories.api.voxel.VoxelDefinition(this, "air", kotlin.collections.mapOf("solid" to "false", "opaque" to "false")))
        this.reloadVoxelTypes()
    }

    private fun reloadVoxelTypes() {
        voxelsByName.clear()
        val gson = Gson()

        /*fun readDefinitions(a: Asset) {
            val loadedVoxels = 0
            logger().debug("Reading voxels definitions in : $a")

            val text = a.reader().use { it.readText() }
            val parser = VoxelDefinitionsParser(CommonTokenStream(VoxelDefinitionsLexer(ANTLRInputStream(text))))

            for(definition in parser.voxelDefinitions().voxelDefinition()) {
                val name = definition.Name().text
                val properties = definition.properties().toMap()

                val voxelDefinition = VoxelDefinition(this, name, properties)
                val voxel : Voxel = voxelDefinition.create()
                voxelsByName[name] = voxel
                logger.debug("Loaded $voxelDefinition from $a, created $voxel")
            }

            logger().debug("Parsed file $a correctly, loading $loadedVoxels voxels.")
        }*/

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
                val voxel : Voxel = voxelDefinition.create()
                voxelsByName[name] = voxel

                logger.debug("Loaded $voxelDefinition from $a, created $voxel")
            }
        }

        air = Voxel(VoxelDefinition(this, "air", mapOf(
                "solid" to "false",
                "opaque" to "false"
        )))
        voxelsByName["air"] = air

        for(asset in content.modsManager().allAssets.filter { it.name.startsWith("voxels/") && !it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun getVoxel(voxelName: String): Voxel? {
        return voxelsByName[voxelName]
    }

    override fun all(): Iterator<Voxel> {
        return voxelsByName.values.iterator()
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

    public fun VoxelDefinitionsParser.PropertiesContext?.toMap(): Map<String, String> {
        if(this == null)
            return emptyMap()

        val map = mutableMapOf<String, String>()

        this.extractIn(map, "")

        return map
    }

    public fun VoxelDefinitionsParser.PropertiesContext.extractIn(map: MutableMap<String, String>, prefix: String) {
        this.property().forEach {
            map.put(prefix + it.Name().text, it.value().getValue())
        }

        this.compoundProperty().forEach {
            map.put(prefix + it.Name().text, "exists")
            it.properties().extractIn(map, prefix + it.Name().text + ".")
        }
    }

    private fun VoxelDefinitionsParser.ValueContext.getValue(): String {
        if (this.Text() != null)
            return this.Text().text.substring(1, this.Text().text.length - 1)
        else return this.text
    }

}
