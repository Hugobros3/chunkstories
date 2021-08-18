//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.content.mods.ModFolderAsset
import xyz.chunkstories.content.mods.ModZipAsset
import xyz.chunkstories.util.FoldersUtils

/**
 * A class to help dumb parsers/loaders that don't have support for loading file
 * through our virtual FS
 */
object AssetAsFileHelper {

    private val logger = LoggerFactory.getLogger("content.cachehelper")

    internal var createCacheFolder = AtomicBoolean(false)
    internal var cacheFolder: File? = null

    fun cacheAsset(asset: Asset, gameInstance: GameInstance): File {
        // Mod folders: we just pass the file
        if (asset is ModFolderAsset) {
            return asset.file
        } else if (asset is ModZipAsset) {
            val cacheFolder = getCacheFolder()

            val extracted = extractAssert(asset, cacheFolder!!)
            // Hack on hack: obj files will require some stuff next to them
            if (asset.name.endsWith(".obj")) {
                val materialFileAsset = gameInstance.content
                        .getAsset(asset.name.substring(0, asset.name.length - 4) + ".mtl")
                if (materialFileAsset != null)
                    extractAssert(materialFileAsset, cacheFolder)
            }

            return extracted
        } else
            throw UnsupportedOperationException("What type is this asset ? $asset")
    }

    private fun extractAssert(asset: Asset, cacheFolder: File): File {
        logger.debug("Extract asset " + asset.name + " to " + cacheFolder)
        try {
            val extractTo = File(cacheFolder.absolutePath + "/" + asset.name)
            val fos = FileOutputStream(extractTo)
            val `is` = asset.read()
            val buffer = ByteArray(4096)
            while (`is`.available() > 0) {
                val r = `is`.read(buffer)
                fos.write(buffer, 0, r)
            }
            `is`.close()
            fos.close()

            return extractTo
        } catch (e: IOException) {
            throw RuntimeException("")
        }

    }

    private fun getCacheFolder(): File? {
        // Obtain a cache folder
        if (createCacheFolder.compareAndSet(false, true)) {
            cacheFolder = File("." + "/cache/assimp/" + (Math.random() * 10000).toInt())
            cacheFolder!!.mkdirs()
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    println("Deleting cache folder " + cacheFolder!!)
                    FoldersUtils.deleteFolder(cacheFolder)
                }
            })
        }

        return cacheFolder
    }
}
