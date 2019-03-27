//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

import xyz.chunkstories.api.content.Asset

object AssetToByteBufferHelper {

    fun loadIntoByteBuffer(asset: Asset): ByteBuffer {
        try {
            val baos = ByteArrayOutputStream()

            val `is` = asset.read()
            val buffer = ByteArray(4096)
            while (`is`.available() > 0) {
                val r = `is`.read(buffer)
                baos.write(buffer, 0, r)
            }
            `is`.close()

            val bytes = baos.toByteArray()

            val bb = ByteBuffer.allocateDirect(bytes.size)
            bb.put(bytes)
            bb.flip()

            return bb
        } catch (e: IOException) {
            throw RuntimeException("Couldn't fully read asset")
        }

    }
}
