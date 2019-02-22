package xyz.chunkstories.util

import java.awt.image.BufferedImage
import java.nio.ByteBuffer

fun BufferedImage.toByteBuffer() : ByteBuffer {
    val image = this

    val data = IntArray(image.width * image.height)
    image.getRGB(0, 0, image.width, image.height, data, 0, image.width);

    val buffer = ByteBuffer.allocateDirect(4 * image.width * image.height)

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val pixel = data[y * image.width + x]
            buffer.put((pixel shr 16 and 0xFF).toByte())
            buffer.put((pixel shr 8 and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
            buffer.put((pixel shr 24 and 0xFF).toByte())
        }
    }

    buffer.flip()

    return buffer
}