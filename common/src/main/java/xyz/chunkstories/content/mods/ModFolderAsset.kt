package xyz.chunkstories.content.mods

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.mods.Mod
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class ModFolderAsset(override val source: ModFolder, override val name: String, file: File) : Asset {

    var file: File
        internal set

    init {
        this.file = file
    }

    override fun read(): InputStream {
        try {
            return FileInputStream(file)
        } catch (e: IOException) {
            source.logger()!!.warn("Failed to read asset : " + name + " from " + source)
            e.printStackTrace()
            throw e
        }
    }

    override fun toString(): String {
        return "[Asset: " + name + " from mod " + source + "]"
    }
}