//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.block

import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import java.util.*

class BlockTypesStore(override val content: GameContentStore) : Content.BlockTypes {
    val textures: BlockTextures

    var byName: MutableMap<String, BlockType> = HashMap()
    override lateinit var air: BlockType

    init {
        val backend = (content.engine as? Client)?.graphics?.backend

        textures = when (backend) {
            is BlockTexturesProvider ->
                backend.createVoxelTextures(this)
            else ->
                HeadlessBlockTexturesStore(this)
        }
    }

    fun reload() {
        this.textures.reload()

        air = BlockType("air", Json.Dict(mapOf(
                "solid" to Json.Value.Bool(false),
                "opaque" to Json.Value.Bool(false)
        )), content)
        this.reloadJsonFiles()

        for(voxel in content.items.parent.blockTypes.all) {
            for(variantDefinition in voxel.variants) {
                content.items.itemDefinitions[variantDefinition.name] = variantDefinition
            }
        }
    }

    private fun reloadJsonFiles() {
        byName.clear()
        fun readDefinitions(a: Asset) {
            logger.debug("Reading blocks definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["blocks"].asDict ?: throw Exception("This json doesn't contain an 'blocks' dict")

            for (element in dict.elements) {
                val name = element.key
                val definition = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                val clazz = definition["class"].asString?.let {
                    content.modsManager.getClassByName(it)?.let {
                        if(BlockType::class.java.isAssignableFrom(it))
                            it as Class<BlockType>
                        else
                            throw Exception("The custom class has to extend the Voxel class !")
                    }
                }  ?: BlockType::class.java

                val constructor = try {
                    clazz.getConstructor(String::class.java, Json.Dict::class.java, this::class.java)
                } catch (e: NoSuchMethodException) {
                    throw Exception("Your custom class, $clazz, lacks the correct BlockType(String, Json.Dict, Content) constructor.")
                }

                val block = constructor.newInstance(this)
                byName[name] = block

                logger.debug("Loaded $block from $a")
            }
        }

        air = BlockType("air", Json.Dict(mapOf(
                "solid" to Json.Value.Bool(false),
                "opaque" to Json.Value.Bool(false)
        )), content)
        byName["air"] = air

        for (asset in content.modsManager.allAssets.filter { it.name.startsWith("voxels/") && !it.name.startsWith("voxels/materials/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    override fun get(name: String): BlockType? {
        return byName[name]
    }

    override fun getTexture(name: String) = textures.getTexture(name)
    override val defaultTexture: BlockTexture
        get() = textures.defaultTexture

    override val all: Sequence<BlockType>
        get() {
            return byName.values.asSequence()
        }

    companion object {
        private val logger = LoggerFactory.getLogger("content.voxels")
    }

    override val logger: Logger
        get() = Companion.logger
}
