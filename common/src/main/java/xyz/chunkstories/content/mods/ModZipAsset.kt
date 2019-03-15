package xyz.chunkstories.content.mods

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.mods.Mod
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry

class ModZipAsset(override val source: ModZip, override val name: String, internal var entry: ZipEntry) : Asset {

    override fun read(): InputStream {
        try {
            return source.zipFile.getInputStream(entry)
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