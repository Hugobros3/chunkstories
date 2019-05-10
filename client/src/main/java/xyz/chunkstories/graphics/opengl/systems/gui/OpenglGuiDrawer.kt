package xyz.chunkstories.graphics.opengl.systems.gui

import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.common.DummyGuiDrawer
import xyz.chunkstories.graphics.opengl.OpenglFrame
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem

class OpenglGuiDrawer(pass: OpenglPass) : OpenglDrawingSystem(pass) {

    override fun executeDrawingCommands(frame: OpenglFrame, ctx: SystemExecutionContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}