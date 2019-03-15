package xyz.chunkstories.graphics.vulkan.textures.voxels

import org.joml.Vector4f
import org.joml.Vector4fc
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.voxel.ReloadableVoxelTextures
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

open class VoxelTexturesArray(val voxels: Content.Voxels) : ReloadableVoxelTextures {

    val content = voxels.parent()

    private val voxelTextures = mutableMapOf<String, VoxelTextureInArray>()

    override lateinit var defaultVoxelTexture: VoxelTexture

    override val all: Collection<VoxelTexture>
        get() = voxelTextures.values

    init {
        //reload()
    }

    override fun reload() {
        voxelTextures.clear()

        for (entry in content.modsManager().allUniqueEntries) {
            if (entry.name.startsWith("voxels/textures/")) {
                val name = entry.name.replace("voxels/textures/", "")

                val asset = entry.topInstance
                // For now only PNG is supported TODO: .hdr and more ?
                if (asset.name.endsWith(".png")) {
                    val textureName = name.replace(".png", "")/*.replace("/", ".").replace("\\", ".")*/
                    if (textureName.endsWith("_normal") || textureName.endsWith("_roughness") || textureName.endsWith("_metalness")
                            || textureName.endsWith("_n") || textureName.endsWith("_r") || textureName.endsWith("_m")) {
                        // Don't create entries for complementary textures!
                        continue
                    }

                    if (asset.name.contains("/_"))
                        continue // ignore directories starting with _

                    val voxelTexture = VoxelTextureInArray(textureName, asset)
                    voxelTextures[textureName] = voxelTexture
                }
            }
        }

        defaultVoxelTexture = voxelTextures["notex"]!!

        createVoxelArray()
    }

    fun createVoxelArray() {
        var minTextureSize = Int.MAX_VALUE
        var maxTextureSize = Int.MIN_VALUE

        val minAcceptableTextureResolution = 16
        val maxAcceptableTextureResolution = 32

        val todo = voxelTextures.values.toList()

        var i = 0
        for (voxelTexture in todo) {
            voxelTexture.textureArrayIndex = i++

            if (voxelTexture.imageSize < minTextureSize)
                minTextureSize = voxelTexture.imageSize

            if (voxelTexture.imageSize > maxTextureSize)
                maxTextureSize = voxelTexture.imageSize
        }

        val textureResolution = clamp(maxTextureSize, minAcceptableTextureResolution, maxAcceptableTextureResolution)
        logger.debug("Using voxel texture resolution: $textureResolution")

        val images = mutableMapOf<Asset, BufferedImage>()
        fun image(asset: Asset) = images.getOrPut(asset) {
            val originalImage = ImageIO.read(asset.read())
            if (originalImage.width == textureResolution)
                return originalImage
            else {
                val image2 = BufferedImage(textureResolution, textureResolution, originalImage.type)
                //logger.debug("Resizing image $asset")

                //TODO this is the world's worst nearest-neighbor ever
                for (x in 0 until image2.width)
                    for (y in 0 until image2.height) {
                        val sx = clamp((x / image2.width.toFloat() * originalImage.width.toFloat()).toInt(), 0, originalImage.width - 1)
                        val sy = clamp((y / image2.height.toFloat() * originalImage.height.toFloat()).toInt(), 0, originalImage.height - 1)
                        val rgb = originalImage.getRGB(sx, sy)
                        image2.setRGB(x, y, rgb)
                    }

                return image2
            }
        }

        val mappedVoxelImageData: List<Array<BufferedImage>> = todo.map {
            arrayOf(
                    image(it.asset),
                    image(it.normal),
                    image(it.metalness),
                    image(it.roughness)
            )
        }

        createTextureArray(textureResolution, mappedVoxelImageData)
    }

    open fun createTextureArray(textureResolution: Int, imageData: List<Array<BufferedImage>>) {
        // Nothing lol xd
    }

    private fun clamp(i: Int, min: Int, max: Int): Int = when {
        i > max -> max
        i < min -> min
        else -> i
    }

    inner class VoxelTextureInArray(override val name: String, val asset: Asset) : VoxelTexture {
        override val animationFrames: Int = 1 //TODO support animations ?

        override var color: Vector4fc = Vector4f(1f)
            internal set
        var imageSize = 0
            internal set

        var textureArrayIndex = 0
            internal set

        override val textureScale: Int = 1//TODO read json stuff

        val normal: Asset
        val roughness: Asset
        val metalness: Asset

        init {
            val assetName = asset.name
            val strippedAssetName = assetName.substring(0, assetName.length - ".png".length)

            analyzeAsset(asset)

            normal = content.modsManager().getAsset(strippedAssetName + "_normal.png")
                    ?: content.modsManager().getAsset(strippedAssetName + "_n.png")
                            ?: content.modsManager().getAsset("voxels/textures/notex_normal.png")!!

            roughness = content.modsManager().getAsset(strippedAssetName + "_roughness.png")
                    ?: content.modsManager().getAsset(strippedAssetName + "_r.png")
                            ?: content.modsManager().getAsset("voxels/textures/notex_roughness.png")!!

            metalness = content.modsManager().getAsset(strippedAssetName + "_metalness.png")
                    ?: content.modsManager().getAsset(strippedAssetName + "_m.png")
                            ?: content.modsManager().getAsset("voxels/textures/notex_metalness.png")!!
        }

        private fun analyzeAsset(asset: Asset) {
            val image = ImageIO.read(asset.read())

            imageSize = image.width

            var redAcc = 0f
            var greenAcc = 0f
            var blueAcc = 0f
            var alphaAcc = 0f
            for (x in 0 until image.width)
                for (y in 0 until image.height) {
                    val rgb = image.getRGB(x, y)

                    //TODO support HDR properly
                    val red = (rgb and 0xFF0000 shr 16) / 255f
                    val green = (rgb and 0x00FF00 shr 8) / 255f
                    val blue = (rgb and 0x0000FF) / 255f

                    val alpha = (rgb and -0x1000000).ushr(24) / 255f
                    if (alpha > 0) {
                        redAcc += red
                        greenAcc += green
                        blueAcc += blue
                        alphaAcc += alpha
                    }
                }

            color = if (alphaAcc > 0) {
                Vector4f(redAcc / alphaAcc, greenAcc / alphaAcc, blueAcc / alphaAcc, 1f)
            } else {
                Vector4f(0f)
            }
        }
    }

    override fun get(voxelTextureName: String): VoxelTexture {
        return voxelTextures[voxelTextureName] ?: defaultVoxelTexture
    }

    override fun logger() = logger

    override fun parent() = voxels

    companion object {
        val logger = LoggerFactory.getLogger("content.voxels.textures")
    }
}