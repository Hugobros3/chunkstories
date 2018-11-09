package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.client.LocalPlayer
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory
import io.xol.chunkstories.api.entity.traits.serializable.TraitRotation
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.input.lwjgl3.Lwjgl3KeyBind
import io.xol.chunkstories.util.VersionInfo
import io.xol.chunkstories.world.WorldImplementation

class DebugInfoRendererHelper(ingameLayer: IngameLayer) {
    val gui = ingameLayer.gui

    fun drawDebugInfo(guiDrawer: GuiDrawer) {
        var posY = gui.viewportHeight - 4
        val font = guiDrawer.fonts.getFont("LiberationSans-Regular", 10f)

        fun debugLine(text: String) {
            posY -= 12
            guiDrawer.drawStringWithShadow(font, 4, posY, text)
        }

        val client = gui.client.ingame!!
        val window = (client.gameWindow as GLFWWindow)
        val world = client.world as WorldImplementation

        debugLine("Chunk Stories ${VersionInfo.version} running on the ${window.graphicsBackend.javaClass.simpleName}")
        debugLine("#FF0000Rendering performance : todo FPS | ms #00FFFFSimulation performance : ${world.gameLogic.simulationFps}")
        debugLine("World info : ${world.allLoadedChunks.count()} chunks loaded, ${world.regionsHolder.stats}")

        val playerEntity = client.player.controlledEntity
        if(playerEntity != null ) {
            debugLine("Controlled entity id ${playerEntity.UUID} position ${playerEntity.location} type ${playerEntity.definition.name}")
            debugLine("Looking at ${playerEntity.traits[TraitRotation::class]?.directionLookingAt}")

            debugLine("controller focus: ${(playerEntity.traits[TraitControllable::class.java]?.controller as? LocalPlayer)?.hasFocus()}")
        }

        debugLine("gui focus ${gui.hasFocus()}")
        debugLine("input mgr focus ${client.inputsManager.mouse.isGrabbed}")

        val inp = client.inputsManager.getInputByName("forward") as Lwjgl3KeyBind
        debugLine("forward key ${inp.name} ${inp.isPressed}")
    }
}