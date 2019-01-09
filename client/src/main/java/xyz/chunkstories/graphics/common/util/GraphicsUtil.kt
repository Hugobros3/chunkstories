package xyz.chunkstories.graphics.common.util

import java.awt.image.BufferedImage
import java.nio.ByteBuffer

fun BufferedImage.toByteBuffer(): ByteBuffer {
    val data = IntArray(width * height)
    getRGB(0, 0, width, height, data, 0, width)

    val buffer = ByteBuffer.allocateDirect(4 * width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = data[y * width + x]
            buffer.put((pixel shr 16 and 0xFF).toByte())
            buffer.put((pixel shr 8 and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
            buffer.put((pixel shr 24 and 0xFF).toByte())
        }
    }

    buffer.flip()

    return buffer
}
