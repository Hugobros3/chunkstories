//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.mods

import java.io.File
import java.util.HashMap

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import xyz.chunkstories.api.util.IterableIterator

class ModFolder @Throws(ModLoadFailureException::class)
constructor(internal val folder: File) : ModImplementation() {
    private val assetsMap: MutableMap<String, ModFolderAsset> = HashMap()

    override val loadString: String
        get() = folder.absolutePath

    override fun toString(): String {
        return "[ModFolder: " + folder.absolutePath + "]"
    }

    init {

        recursiveFolderRead(folder)

        this.modInfo = loadModInfo(getAssetByName("modInfo.json")!!.reader())
        // loadModInformation();
        logger = LoggerFactory.getLogger("mod." + this.modInfo.internalName)
    }

    private fun recursiveFolderRead(file: File) {
        if (file.isDirectory) {
            for (f in file.listFiles()!!)
                recursiveFolderRead(f)
        } else {
            var fileName = file.absolutePath.substring(folder.absolutePath.length + 1)
            fileName = fileName.replace('\\', '/')
            val assetName = fileName

            assetsMap[assetName] = ModFolderAsset(this, assetName, file)
        }
    }

    override fun getAssetByName(name: String): Asset? {
        return assetsMap[name]
    }

    override fun close() {}

    override val assets: Sequence<Asset>
        get() = assetsMap.values.asSequence()

}
