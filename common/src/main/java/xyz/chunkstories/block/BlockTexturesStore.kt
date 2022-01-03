package xyz.chunkstories.block

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.util.graphics.averageColor
import javax.imageio.ImageIO

interface BlockTexturesProvider {
    fun createVoxelTextures(blockTypes: BlockTypesStore): BlockTextures
}

interface BlockTextures {
    fun getTexture(textureName: String): BlockTexture?
    fun getTextureOrDefault(textureName: String): BlockTexture
    val defaultTexture: BlockTexture
    fun reload()
}

abstract class BlockTexturesStore(val parent: BlockTypesStore) : BlockTextures {
    val content = parent.content

    override fun reload() {
        for (asset in content.modsManager.allAssets) {
            if (asset.name.startsWith("voxels/textures/") || asset.name.startsWith("blocks/textures/")) {
                val name = asset.name.replace("voxels/textures/", "").replace("blocks/textures/", "")

                if (asset.name.endsWith(".png")) {
                    val textureName = name.replace(".png", "").replace("/", ".").replace("\\", ".")
                    if (textureName.endsWith("_normal") || textureName.endsWith("_roughness") || textureName.endsWith("_metalness")
                            || textureName.endsWith("_n") || textureName.endsWith("_r") || textureName.endsWith("_m")) {
                        // Don't create entries for complementary textures!
                        continue
                    }

                    // ignore directories starting with _
                    if (asset.name.contains("/_"))
                        continue

                    loadTexture(textureName, asset)
                }
            }
        }
    }

    protected abstract fun loadTexture(textureName: String, asset: Asset)

    override fun getTextureOrDefault(textureName: String) = getTexture(textureName) ?: defaultTexture

    companion object {
        val logger: Logger = LoggerFactory.getLogger("content.blocks.textures")
    }
}

class HeadlessBlockTexturesStore(parent: BlockTypesStore) : BlockTexturesStore(parent) {
    private val blockTextures = mutableMapOf<String, BlockTexture>()
    private var nextId = 0

    override fun reload() {
        blockTextures.clear()
        nextId = 0
        super.reload()
        defaultTexture = blockTextures["notex"]!!
    }

    override fun loadTexture(textureName: String, asset: Asset) {
        val image = ImageIO.read(asset.read())
        val averagedColor = averageColor(image)
        blockTextures[textureName] = BlockTexture(textureName, nextId++, averagedColor, 1, 0)
    }

    override lateinit var defaultTexture: BlockTexture
    override fun getTexture(textureName: String): BlockTexture? {
        return blockTextures[textureName]
    }
}