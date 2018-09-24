package io.xol.chunkstories.gui

import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.api.gui.Fonts

class FontsLibrary(val content: Content) : Fonts {
    val fonts = mutableMapOf<Pair<String, Float>, Font>()
    val defaultFont = TrueTypeFont(content.getAsset("fonts/LiberationSans-Regular.ttf"), 12F, false)

    private fun loadFont(fontName: String, sizeInPx: Float) : Font? {
        val asset = content.getAsset("fonts/$fontName.ttf") ?: return null
        val font = TrueTypeFont(asset, sizeInPx, false)
        fonts[Pair(fontName, sizeInPx)] = font
        return font
    }

    override fun defaultFont(): Font = defaultFont

    override fun defaultFont(sizeMultiplier: Int): Font = getFont("fonts/LiberationSans-Regular.ttf", 12F * sizeMultiplier)

    override fun getFont(fontName: String, sizeInPX: Float): Font =
        fonts.getOrPut(Pair(fontName, sizeInPX)) {
            loadFont(fontName, sizeInPX) ?: (defaultFont.apply { println("Could find font $fontName, substituting the default font") })
        }

}