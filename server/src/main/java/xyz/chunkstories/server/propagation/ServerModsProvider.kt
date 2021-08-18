//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.propagation

import xyz.chunkstories.server.DedicatedServer
import java.util.HashMap
import java.lang.Thread
import xyz.chunkstories.util.FoldersUtils
import xyz.chunkstories.content.mods.ModZip
import xyz.chunkstories.content.mods.ModFolder
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.io.IOException
import java.lang.UnsupportedOperationException

/**
 * Provides mods for connected users
 */
class ServerModsProvider(server: DedicatedServer) {
    // The mods string is just the list of md5 hashes of the mods enabled on the server
    private val cacheFolder: File
    private val redistribuables: MutableMap<String, File> = HashMap()
    var modsString: String
    fun obtainModRedistribuable(md5: String?): File? {
        return redistribuables[md5]
    }

    init {
        server.logger.info("Starting to build server mods cache to provide to users")
        cacheFolder = File("./cache/servermods-" + (Math.random() * 100000).toInt() + "/")
        cacheFolder.mkdirs()
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                println("Deleting servermods cache folder $cacheFolder")
                FoldersUtils.deleteFolder(cacheFolder)
            }
        })

        // Build the modstring
        modsString = ""
        for (mod in server.content.modsManager.currentlyLoadedMods) {
            val hash = mod.hash
            var size: Long
            server.logger.info("Building distribuable zipfile for mod " + mod.modInfo.name)
            if (mod is ModZip) {
                server.logger.info("Nevermind, that mod is already in a .zip format, moving on")
                redistribuables[hash] = mod.zipFileLocation
                size = mod.zipFileLocation.length()
            } else if (mod is ModFolder) {
                server.logger.info("Making it from scratch.")
                val wipZipfile = File(cacheFolder.absolutePath + "/" + hash + ".zip")
                try {
                    val fos = FileOutputStream(wipZipfile)
                    val zos = ZipOutputStream(fos)
                    val buffer = ByteArray(4096)
                    for (asset in mod.assets) {
                        val entry = ZipEntry(asset.name.substring(2))
                        zos.putNextEntry(entry)
                        val `is` = asset.read()
                        var red: Int
                        while (`is`.read(buffer).also { red = it } > 0) zos.write(buffer, 0, red)
                        `is`.close()
                    }
                    zos.closeEntry()
                    zos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                redistribuables[hash] = wipZipfile
                size = wipZipfile.length()
            } else throw UnsupportedOperationException("Mods can't be anything but a .zip or a folder")

            // Also add it to the string
            modsString += mod.modInfo.internalName + ":" + hash + ":" + size + ";"
        }

        // Remove the last ;
        if (modsString.length > 1) modsString = modsString.substring(0, modsString.length - 1)
    }
}