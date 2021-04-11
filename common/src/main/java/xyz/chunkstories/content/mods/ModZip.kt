//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.mods

import java.io.File
import java.io.IOException
import java.util.HashMap
import java.util.zip.ZipFile

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException

class ModZip @Throws(ModLoadFailureException::class)
constructor(val zipFileLocation: File) : ModImplementation() {
    internal val zipFile: ZipFile
    private val assetsMap: MutableMap<String, ModZipAsset> = HashMap()

    override val loadString: String
        get() = zipFileLocation.absolutePath

    override fun toString(): String {
        return "[ModZip: " + zipFileLocation.absolutePath + "]"
    }

    init {
        try {
            this.zipFile = ZipFile(zipFileLocation)

            val e = zipFile.entries()
            while (e.hasMoreElements()) {
                val entry = e.nextElement()
                if (!entry.isDirectory) {
                    val assetName = entry.name
                    assetsMap[assetName] = ModZipAsset(this, assetName, entry)
                }
            }

            this.modInfo = loadModInfo(getAssetByName("modInfo.json")!!.reader())
        } catch (e: IOException) {
            throw ModLoadFailureException(this, "Zip file not found or malformed")
        }

        logger = LoggerFactory.getLogger("mod." + this.modInfo.internalName)
    }

    override fun getAssetByName(name: String): Asset? {
        return assetsMap[name]
    }

    override fun close() {
        try {
            zipFile.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override val assets: Sequence<Asset>
        get() = assetsMap.values.asSequence()
}
