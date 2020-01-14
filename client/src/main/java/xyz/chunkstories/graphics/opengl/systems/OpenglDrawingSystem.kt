package xyz.chunkstories.graphics.opengl.systems

import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance

abstract class OpenglDrawingSystem(val pass: OpenglPass) : DrawingSystem, Cleanable {
    abstract fun executeDrawingCommands(context: OpenglPassInstance)
}