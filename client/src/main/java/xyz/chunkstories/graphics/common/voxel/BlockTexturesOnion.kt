package xyz.chunkstories.graphics.common.voxel

import org.joml.Vector4fc
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.block.BlockTextureID
import xyz.chunkstories.api.math.MathUtils.clamp
import xyz.chunkstories.block.BlockTexturesStore
import xyz.chunkstories.block.BlockTypesStore
import xyz.chunkstories.util.graphics.averageColor
import xyz.chunkstories.util.graphics.resizeImage
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** Used to generate "onion" textures (ie layered/array texture) containing all the block textures */
abstract class BlockTexturesOnion(parent: BlockTypesStore) : BlockTexturesStore(parent) {
    private val voxelTextures = mutableMapOf<String, VoxelTextureInArray>()
    private val voxelTexturesList = mutableListOf<VoxelTextureInArray>()

    private lateinit var defaultTexture_: VoxelTextureInArray

    override val defaultTexture: BlockTexture
        get() = defaultTexture_.blockTexture

    private var nextId = 0

    override fun reload() {
        voxelTextures.clear()
        voxelTexturesList.clear()
        nextId = 0
        super.reload()
        defaultTexture_ = voxelTextures["notex"]!!
        createVoxelArray()
    }

    override fun loadTexture(textureName: String, asset: Asset) {
        val loadedTexture = VoxelTextureInArray(textureName, nextId++, asset)
        voxelTextures[textureName] = loadedTexture
        voxelTexturesList.add(loadedTexture)
    }

    fun createVoxelArray() {
        val minTextureSize = voxelTexturesList.minBy { it.imageSize }!!.imageSize
        val maxTextureSize = voxelTexturesList.maxBy { it.imageSize }!!.imageSize

        val minAcceptableTextureResolution = 16
        val maxAcceptableTextureResolution = 32

        val textureResolution = clamp(maxTextureSize, minAcceptableTextureResolution, maxAcceptableTextureResolution)
        logger.debug("Using voxel texture resolution: $textureResolution")

        val images = mutableMapOf<Asset, BufferedImage>()
        fun image(asset: Asset) = images.getOrPut(asset) {
            val originalImage = ImageIO.read(asset.read())
            when (originalImage.width) {
                textureResolution -> return originalImage
                else -> resizeImage(originalImage, textureResolution, textureResolution)
            }
        }

        val mappedVoxelImageData: List<Array<BufferedImage>> = voxelTexturesList.map {
            arrayOf(
                    image(it.asset),
                    image(it.normal),
                    image(it.metalness),
                    image(it.roughness)
            )
        }

        createTextureArray(textureResolution, mappedVoxelImageData)
    }

    abstract fun createTextureArray(textureResolution: Int, imageData: List<Array<BufferedImage>>)

    inner class VoxelTextureInArray(textureName: String, id: BlockTextureID, val asset: Asset) {
        val color: Vector4fc
        val imageSize: Int
        val textureArrayIndex = id

        @Deprecated("simplify")
        val textureScale: Int = 1

        val normal: Asset
        val roughness: Asset
        val metalness: Asset

        val blockTexture: BlockTexture
        var image: BufferedImage? = null

        init {
            val assetName = asset.name
            val strippedAssetName = assetName.substring(0, assetName.length - ".png".length)

            image = ImageIO.read(asset.read())

            imageSize = image!!.width
            color = averageColor(image!!)

            blockTexture = BlockTexture(textureName, id, color, 1, 0)

            normal = content.modsManager.getAsset(strippedAssetName + "_normal.png")
                    ?: content.modsManager.getAsset(strippedAssetName + "_n.png")
                            ?: content.modsManager.getAsset("voxels/textures/notex_normal.png")!!

            roughness = content.modsManager.getAsset(strippedAssetName + "_roughness.png")
                    ?: content.modsManager.getAsset(strippedAssetName + "_r.png")
                            ?: content.modsManager.getAsset("voxels/textures/notex_roughness.png")!!

            metalness = content.modsManager.getAsset(strippedAssetName + "_metalness.png")
                    ?: content.modsManager.getAsset(strippedAssetName + "_m.png")
                            ?: content.modsManager.getAsset("voxels/textures/notex_metalness.png")!!
        }
    }

    override fun getTexture(textureName: String): BlockTexture? {
        return voxelTextures[textureName]?.blockTexture
    }
}