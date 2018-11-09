package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.util.VersionInfo

class DebugInfoRendererHelper(ingameLayer: IngameLayer) {
    val gui = ingameLayer.gui

    fun drawDebugInfo(guiDrawer: GuiDrawer) {
        var posY = gui.viewportHeight - 4
        val font = guiDrawer.fonts.getFont("LiberationSans-Regular", 10f)

        fun debugLine(text: String) {
            posY -= 12
            guiDrawer.drawString(font, 4, posY, text)
        }

        val client = gui.client.ingame!!
        val window = (client.gameWindow as GLFWWindow)

        debugLine("Chunk Stories ${VersionInfo.version} running on the ${window.graphicsBackend.javaClass.simpleName}")
        debugLine("Performance data : todo FPS | ms ")
        val playerEntity = client.player.controlledEntity
        if(playerEntity != null ) {
            debugLine("Controlled entity id ${playerEntity.UUID} position ${playerEntity.location} type ${playerEntity.definition.name}")
            debugLine("Controlled entity ${playerEntity.traits[TraitInventory::class]}")
        }
    }
}