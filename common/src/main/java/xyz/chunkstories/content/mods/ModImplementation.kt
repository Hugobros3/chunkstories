//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.mods

import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Comparator

import org.slf4j.Logger

import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.content.mods.ModInfo
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import xyz.chunkstories.api.math.HexTools
import xyz.chunkstories.api.util.IterableIterator

abstract class ModImplementation @Throws(ModLoadFailureException::class)
internal constructor() : Mod {
    override lateinit var modInfo: ModInfo
        protected set

    protected var md5hash: String? = null

    override val mD5Hash: String
        get() {
            if (md5hash == null)
                computeMD5Hash()
            return md5hash!!
        }

    abstract val loadString: String

    protected var logger: Logger? = null

    abstract override fun getAssetByName(name: String): Asset?

    abstract override fun assets(): IterableIterator<Asset>

    @Synchronized
    private fun computeMD5Hash() {
        // Makes a sorted list of the names of all the assets
        val assetsSorted = ArrayList<String>()
        for (asset in assets()) {
            assetsSorted.add(asset.name)
        }

        assetsSorted.sort()
        //assetsSorted.sort(Comparator { o1, o2 -> o1.compareTo(o2) })

        // Concatenate their names...
        var completeNamesString = ""
        for (s in assetsSorted)
            completeNamesString += "$s;"

        // MD5 it
        val hashedNames = HexTools.byteArrayAsHexString(md.digest(completeNamesString.toByteArray()))

        // Iterate over each asset, hash it then add that to the sb
        val sb = StringBuilder()
        for (s in assetsSorted) {
            val a = this.getAssetByName(s)
            val buffer = ByteArray(4096)
            val eater = DigestInputStream(a!!.read(), md)
            try {
                while (eater.read(buffer) != -1)
                ;
                eater.close()
            } catch (e: IOException) {

            }

            // Append
            sb.append(HexTools.byteArrayAsHexString(md.digest()))
        }
        // Append hash of list of names
        sb.append(hashedNames)

        // Hash the whole stuff again
        md5hash = HexTools.byteArrayAsHexString(md.digest(sb.toString().toByteArray()))
    }

    abstract fun close()

    fun logger(): Logger? {
        return logger
    }

    companion object {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
    }
}
