//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.math.HexUtils
import java.awt.*
import java.awt.image.BufferedImage
import java.io.IOException

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's
 * Engine, edited for Chunk Stories engine
 *
 * original author James Chambers (Jimmy)
 * original author Jeremy Adams (elias4444)
 * original author Kevin Glass (kevglass)
 * original author Peter Korzuszek (genail)
 * new version edited by David Aaron Muhar (bobjob)
 * new version edited by Hugo Devillers (gobrosse)
 */
//TODO: rip out this bullshit and just use stb_truetype like every other cool kid
class TrueTypeFont @Throws(FontFormatException::class, IOException::class) constructor(fontAsset: Asset, sizeInPX: Float, private val antiAlias: Boolean) : Font {
    val glyphs: Array<Glyph?> = arrayOfNulls(65536)

    override var lineHeight = 0
        private set

    private val font: java.awt.Font

    private val pageCreated = BooleanArray(256)
    private val fontSize: Int

    init {
        logger.debug("Loading font:" + fontAsset + "fontSize: " + sizeInPX)
        font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontAsset.read()).deriveFont(sizeInPX)

        this.fontSize = font.size

        createPage(0)

        lineHeight -= 1
        if (lineHeight <= 0)
            lineHeight = 1
    }

    private fun getFontImage(ch: Char): BufferedImage {
        // Create a temporary image to extract the character's size
        val tempfontImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tempFontGraphics = tempfontImage.graphics as Graphics2D

        if (antiAlias)
            tempFontGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        else
            tempFontGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

        tempFontGraphics.font = font
        val fontMetrics = tempFontGraphics.fontMetrics
        var charwidth = fontMetrics.charWidth(ch)
        if (charwidth <= 0) {
            charwidth = 7
        }
        var charheight = fontMetrics.height + 3
        if (charheight <= 0) {
            charheight = fontSize
        }

        // Create another image holding the character we are creating
        val fontImage: BufferedImage
        fontImage = BufferedImage(charwidth, charheight, BufferedImage.TYPE_INT_ARGB)
        val gt = fontImage.graphics as Graphics2D
        if (antiAlias)
            gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        else
            gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

        gt.font = font
        gt.color = Color.WHITE
        val charx = 0
        val chary = 1
        gt.drawString(ch.toString(), charx, chary + fontMetrics.ascent)

        return fontImage

    }

    fun createPage(page: Int): BufferedImage? {
        // If there are custom chars then I expand the font texture twice

        // In any case this should be done in other way. Texture with size
        // 512x512
        // can maintain only 256 characters with resolution of 32x32. The
        // texture
        // size should be calculated dynamicaly by looking at character sizes.
        try {
            pageCreated[page] = true

            val imgTemp = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
            val g = imgTemp.graphics as Graphics2D

            // WHY Y U DO DIS
            g.color = Color(0, 0, 0, 0)
            g.fillRect(0, 0, textureWidth, textureHeight)

            var rowHeight = 0
            var positionX = 0
            var positionY = 0

            // int customCharsLength = (customCharsArray != null) ?
            // customCharsArray.length : 0;

            for (i in page * 256 until page * 256 + 256) {
                // get 0-255 characters and then custom characters
                val character = i.toChar()

                val fontImage = getFontImage(character)

                if (positionX + fontImage.width + 1 >= textureWidth) {
                    positionX = 0
                    positionY += rowHeight
                    rowHeight = 0
                }
                val glyph = Glyph(character, page, fontImage.width, fontImage.height, positionX, positionY)

                if (glyph.height > lineHeight) {
                    lineHeight = glyph.height
                }

                if (glyph.height > rowHeight) {
                    rowHeight = glyph.height
                }

                // Draw it here
                g.drawImage(fontImage, positionX, positionY, null)

                positionX += glyph.width + 1

                glyphs[i] = glyph
            }

            return imgTemp

        } catch (e: Exception) {
            System.err.println("Failed to create font.")
            e.printStackTrace()
        }

        return null
    }

    override fun getWidth(text: String): Int {
        if (text.contains("\n")) {
            var max = 0
            for (line in text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val l = getWidth(line)
                if (l > max)
                    max = l
            }
            return max
        }

        var totalwidth = 0
        var glyph: Glyph?
        var currentChar: Int
        for (i in 0 until text.length) {
            currentChar = text[i].toInt()

            glyph = glyphs[currentChar]

            if (glyph != null) {
                if (glyph.width < 3)
                    totalwidth += 1
                totalwidth += glyph.width
            }
        }
        return totalwidth
    }

    override fun getLinesHeight(text: String, clipX: Float): Int {
        val clip = clipX != -1f
        var lines = 1
        var i = 0
        var charCurrent: Char
        var glyph: Glyph?
        var totalwidth = 0
        while (i < text.length) {
            charCurrent = text[i]

            if (!pageCreated[charCurrent.toInt() / 256])
                createPage(charCurrent.toInt() / 256)

            glyph = glyphs[charCurrent.toInt()]

            if (glyph != null) {
                if (charCurrent == '#' && text.length - i - 1 >= 6 && text.toCharArray()[i + 1] != '#' && HexUtils
                                .isHexOnly(text.substring(i + 1, i + 7))) {
                    if (!(i > 1 && text.toCharArray()[i - 1] == '#')) {
                        i += 7
                        continue
                    }
                } else if (charCurrent == '\n') {
                    totalwidth = 0
                    lines++
                } else {
                    if (clip && totalwidth + glyph.width > clipX) {
                        lines++
                        totalwidth = 0
                        continue
                    }

                    if (glyph.width < 3)
                        totalwidth += 1
                    totalwidth += glyph.width
                }
                i++
            }
        }
        return lines
    }

    /*public static boolean isSupported(String fontname) {
		java.awt.Font[] font = getFonts();
		for (int i = font.length - 1; i >= 0; i--) {
			if (font[i].getName().equalsIgnoreCase(fontname))
				return true;
		}
		return false;
	}

	public static java.awt.Font[] getFonts() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
	}*/

    override fun size(): Float {
        return fontSize.toFloat()
    }

    companion object {
        const val textureWidth = 512
        const val textureHeight = 512

        private val logger = LoggerFactory.getLogger("rendering.fonts")
    }
}