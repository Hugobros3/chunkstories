package xyz.chunkstories.graphics.opengl.systems.gui

import org.joml.Vector4f
import org.joml.Vector4fc
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.math.HexTools
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.gui.InternalGuiDrawer
import xyz.chunkstories.graphics.common.util.toByteBuffer
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D
import xyz.chunkstories.gui.Glyph
import xyz.chunkstories.gui.TrueTypeFont

class OpenglFontRenderer (val backend: OpenglGraphicsBackend) : Cleanable {
    val texturePages = mutableMapOf<Font, MutableMap<Int, OpenglTexture2D>>()

    fun drawString(drawer: InternalGuiDrawer, font: Font, x: Float, y: Float, whatchars: String, clipX: Float, color: Vector4fc) {
        val trueTypeFont = font as TrueTypeFont
        val scaleX = 1f
        val scaleY = 1f

        val clip = clipX != -1f

        var glyph: Glyph?
        var charCurrent: Int

        var totalwidth = 0
        var i = 0
        var startY = 0f

        var colorModified = Vector4f(color)
        val lines = whatchars.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (lines.size == 0)
            return
        var lineI = 0

        /*int currentLineTotalLength = trueTypeFont.getWidth(lines[lineI]);
		if (alignement == ALIGN_CENTER) {
			if (currentLineTotalLength < clipX / scaleX)
				totalwidth += (clipX / scaleX - currentLineTotalLength) / 2;
		}*/
        var charsThisLine = 0
        while (i < whatchars.length) {
            charCurrent = whatchars[i].toInt()

            /*Texture2DGL pageTexture = trueTypeFont.glTextures[charCurrent / 256];
			// Generates any required unicode page
			if (pageTexture == null)
				pageTexture = trueTypeFont.createPage(charCurrent / 256);*/

            val pageId = charCurrent / 256
            fun createPage() : OpenglTexture2D {
                val image = trueTypeFont.createPage(charCurrent / 256)

                val byteBuffer = image!!.toByteBuffer()

                val pageTexture = OpenglTexture2D(
                        backend,
                        TextureFormat.RGBA_8,
                        TrueTypeFont.textureWidth,
                        TrueTypeFont.textureHeight
                )

                pageTexture.upload(byteBuffer)

                texturePages.getOrPut(font) { mutableMapOf()}[pageId] = pageTexture

                return pageTexture
            }

            var pageTexture = texturePages[font]?.get(pageId) ?: createPage()

            glyph = trueTypeFont.glyphs[charCurrent]

            if (glyph != null) {
                // Detects and parses #C0L0R codes
                if (charCurrent == '#'.toInt() && whatchars.length - i - 1 >= 6 && whatchars.toCharArray()[i + 1] != '#'
                        && HexTools.isHexOnly(whatchars.substring(i + 1, i + 7))) {
                    if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#')) {
                        val colorCode = whatchars.substring(i + 1, i + 7)
                        val rgb = ColorsTools.hexToRGB(colorCode)
                        colorModified = Vector4f(rgb!![0] / 255.0f * color.x(), rgb[1] / 255.0f * color.y(),
                                rgb[2] / 255.0f * color.z(), color.w())
                        i += 7
                        continue
                    }
                } else if (charCurrent == '\n'.toInt()) {
                    startY -= trueTypeFont.height.toFloat()
                    totalwidth = 0
                    charsThisLine = 0

                    if (lineI < lines.size - 1)
                        lineI++
                    /*currentLineTotalLength = trueTypeFont.getWidth(lines[lineI]);
					if (alignement == ALIGN_CENTER) {
						if (currentLineTotalLength < clipX / scaleX)
							totalwidth += (clipX / scaleX - currentLineTotalLength) / 2;
					}*/
                } else {
                    if (clip && totalwidth + glyph.width > clipX / scaleX) {
                        startY -= trueTypeFont.height.toFloat()
                        totalwidth = 0
                        charsThisLine = 0

                        if(charsThisLine > 0)
                            continue
                    }

                    charsThisLine++
                    drawer.drawQuad(totalwidth * scaleX + x, startY * scaleY + y, glyph.width * scaleX,
                            glyph.height * scaleY, glyph.x.toFloat() / TrueTypeFont.textureWidth,
                            (glyph.y + glyph.height).toFloat() / TrueTypeFont.textureHeight,
                            (glyph.x + glyph.width).toFloat() / TrueTypeFont.textureWidth, glyph.y.toFloat() / TrueTypeFont.textureHeight,
                            pageTexture, colorModified)

                    // spacing for looks
                    if (glyph.width < 3)
                        totalwidth += 1

                    totalwidth += glyph.width
                }
                i++
            }
        }
    }

    override fun cleanup() {
        texturePages.forEach {
            it.value.values.forEach(Cleanable::cleanup)
        }
    }
}