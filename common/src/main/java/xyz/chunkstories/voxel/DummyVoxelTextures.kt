package xyz.chunkstories.voxel

import org.joml.Vector4f
import org.joml.Vector4fc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import javax.imageio.ImageIO

interface VoxelTexturesSupport {
    fun createVoxelTextures(voxels: Content.Voxels): ReloadableVoxelTextures
}

interface ReloadableVoxelTextures: Content.Voxels.VoxelTextures {
    fun reload()
}

class DummyVoxelTextures(override val parent: Content.Voxels) : ReloadableVoxelTextures {
    val content = parent.parent

    private val voxelTextures = mutableMapOf<String, DummyVoxelTexture>()

    init {
        //reload()
    }

    override fun reload() {
        voxelTextures.clear()
        for (entry in content.modsManager.allUniqueEntries) {
            if (entry.name.startsWith("voxels/textures/")) {
                val name = entry.name.replace("voxels/textures/", "")

                val asset = entry.topInstance
                // For now only PNG is supported TODO: .hdr and more ?
                if (asset.name.endsWith(".png")) {
                    val textureName = name.replace(".png", "").replace("/", ".").replace("\\", ".")
                    if (textureName.endsWith("_normal") || textureName.endsWith("_roughness") || textureName.endsWith("_metalness")
                            || textureName.endsWith("_n") || textureName.endsWith("_r") || textureName.endsWith("_m")) {
                        // Don't create entries for complementary textures!
                        continue
                    }

                    if (asset.name.contains("/_"))
                        continue // ignore directories starting with _

                    val voxelTexture = DummyVoxelTexture(textureName, asset)
                    voxelTextures[textureName] = voxelTexture
                }
            }
        }

        defaultVoxelTexture = voxelTextures["notex"]!!
    }

    open class DummyVoxelTexture(override val name: String, val asset: Asset) : VoxelTexture {
        override val animationFrames: Int = 1 //TODO support animations ?

        override var color: Vector4fc = Vector4f(1f)
            internal set

        override val textureScale: Int = 1//TODO read json stuff

        init {
            color = getAverageColorFromAsset(asset)
        }

        //TODO move 2 util
        fun getAverageColorFromAsset(asset: Asset): Vector4fc {
            val image = ImageIO.read(asset.read())

            var redAcc = 0f
            var greenAcc = 0f
            var blueAcc = 0f
            var alphaAcc = 0f
            for(x in 0 until image.width)
                for(y in 0 until image.height) {
                    val rgb = image.getRGB(x, y)

                    //TODO support HDR properly
                    val red = (rgb and 0xFF0000 shr 16) / 255f
                    val green = (rgb and 0x00FF00 shr 8) / 255f
                    val blue = (rgb and 0x0000FF) / 255f

                    val alpha = (rgb and -0x1000000).ushr(24) / 255f
                    if(alpha > 0) {
                        redAcc += red
                        greenAcc += green
                        blueAcc += blue
                        alphaAcc += alpha
                    }
                }

            return if(alphaAcc > 0) {
                Vector4f(redAcc / alphaAcc, greenAcc / alphaAcc, blueAcc / alphaAcc, 1f)
            } else {
                Vector4f(0f)
            }
        }
    }

    override lateinit var defaultVoxelTexture: VoxelTexture

    override val all: Collection<VoxelTexture>
        get() = voxelTextures.values

    override fun get(voxelTextureName: String): VoxelTexture {
        return voxelTextures[voxelTextureName] ?: defaultVoxelTexture
    }

    companion object {
        val logger = LoggerFactory.getLogger("content.voxels.textures")
    }

    override val logger: Logger
        get() = Companion.logger
}