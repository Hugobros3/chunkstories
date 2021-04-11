package xyz.chunkstories.block

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.util.graphics.averageColor
import javax.imageio.ImageIO

interface BlockTexturesProvider {
    fun createVoxelTextures(voxels: Content.BlockTypes): BlockTextures
}

interface BlockTextures {
    fun getTexture(name: String): BlockTexture?
    val defaultTexture: BlockTexture
    fun reload()
}

class DummyVoxelTextures(val parent: Content.BlockTypes) : BlockTextures {
    val content = parent.content

    private val voxelTextures = mutableMapOf<String, BlockTexture>()

    override fun reload() {
        voxelTextures.clear()
        for (asset in content.modsManager.allAssets) {
            if (asset.name.startsWith("voxels/textures/")) {
                val name = asset.name.replace("voxels/textures/", "")

                if (asset.name.endsWith(".png")) {
                    val textureName = name.replace(".png", "").replace("/", ".").replace("\\", ".")
                    if (textureName.endsWith("_normal") || textureName.endsWith("_roughness") || textureName.endsWith("_metalness")
                            || textureName.endsWith("_n") || textureName.endsWith("_r") || textureName.endsWith("_m")) {
                        // Don't create entries for complementary textures!
                        continue
                    }

                    if (asset.name.contains("/_"))
                        continue

                    val image = ImageIO.read(asset.read())
                    val averagedColor = averageColor(image)
                    voxelTextures[textureName] = BlockTexture(textureName, averagedColor, 1, 0)
                }
            }
        }

        defaultTexture = voxelTextures["notex"]!!
    }

    val all: Collection<BlockTexture>
        get() = voxelTextures.values

    override lateinit var defaultTexture: BlockTexture
    override fun getTexture(name: String): BlockTexture? {
        return voxelTextures[name]
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger("content.voxels.textures")
    }

    val logger: Logger
        get() = Companion.logger
}