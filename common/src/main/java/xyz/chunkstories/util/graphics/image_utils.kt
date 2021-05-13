package xyz.chunkstories.util.graphics

import org.joml.Vector4f
import xyz.chunkstories.api.math.MathUtils.clamp
import java.awt.image.BufferedImage

fun BufferedImage.getRGBVec4f(x: Int, y: Int): Vector4f {
    val rgb = getRGB(x, y)
    val red = (rgb and 0xFF0000 shr 16) / 255f
    val green = (rgb and 0x00FF00 shr 8) / 255f
    val blue = (rgb and 0x0000FF) / 255f
    val alpha = (rgb and -0x1000000).ushr(24) / 255f
    return Vector4f(red, green, blue, alpha)
}

fun averageColor(image: BufferedImage, weightAlpha: Boolean = true): Vector4f {
    var accumulator = Vector4f(0f)
    for(x in 0 until image.width)
        for(y in 0 until image.height) {
            val pixelColor = image.getRGBVec4f(x, y)
            if (weightAlpha) {
                pixelColor.x = pixelColor.x * pixelColor.w
                pixelColor.y = pixelColor.y * pixelColor.w
                pixelColor.z = pixelColor.z * pixelColor.w
            }
            accumulator = accumulator.add(pixelColor)
        }

    if (accumulator.w > 0)
        accumulator.mul(1.0f / accumulator.w)

    return accumulator
}

/** Low quality NN scaling */
fun resizeImage(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
    val resizedImage = BufferedImage(width, height, originalImage.type)
    for (x in 0 until resizedImage.width)
        for (y in 0 until resizedImage.height) {
            val sx = clamp((x / resizedImage.width.toFloat() * originalImage.width.toFloat()).toInt(), 0, originalImage.width - 1)
            val sy = clamp((y / resizedImage.height.toFloat() * originalImage.height.toFloat()).toInt(), 0, originalImage.height - 1)
            val rgb = originalImage.getRGB(sx, sy)
            resizedImage.setRGB(x, y, rgb)
        }

    return resizedImage
}