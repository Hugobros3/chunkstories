package xyz.chunkstories.gui.debug

import org.joml.Vector4f
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.gui.ClientGui

object FrametimesGraph {

    val size = 512
    val data = LongArray(size)
    var p = 0

    fun receive(delta: Long) {
        data[p] = delta
        p++
        p %= size
    }

    fun draw(guiDrawer: GuiDrawer) {
        (guiDrawer.gui as ClientGui).guiScaleOverride = 1

        val color = Vector4f(2f, 1f, 0f, 1f)

        //println("kéké")

        for(x in 0 until size) {
            guiDrawer.drawBox(x * 1, 0, 1, (data[x].toInt() / 1000) / 100, "textures/gui/white.png", color)
            //println(data[x] / 100000)
        }

        guiDrawer.drawBox(0, 0, size, 166, "textures/gui/white.png", Vector4f(0f, 1f, 0f, 0.25f))

        (guiDrawer.gui as ClientGui).guiScaleOverride = -1
    }
}