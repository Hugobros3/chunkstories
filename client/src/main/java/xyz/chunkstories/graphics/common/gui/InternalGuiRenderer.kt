package xyz.chunkstories.graphics.common.gui

import org.joml.Vector4fc
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.graphics.common.DummyGuiDrawer

abstract class InternalGuiDrawer(gui: Gui) : DummyGuiDrawer(gui) {
    abstract fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: Texture2D, color: Vector4fc?)
}