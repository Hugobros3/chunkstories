//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.config

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList

import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.localization.LocalizationManagerImplementation
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW

import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseScroll

class LanguageSelectionScreen(gui: Gui, parent: Layer, private val allowBackButton: Boolean) : Layer(gui, parent) {
    private val backOption = LargeButtonWithIcon(this, "back")
    private val languages = ArrayList<LanguageButton>()

    private var scroll = 0

    init {

        backOption.action = Runnable { gui.popTopLayer() }

        if (allowBackButton)
            elements.add(backOption)

        for (localization in (gui.localization() as LocalizationManagerImplementation).listTranslations()) {
            val langButton = LanguageButton(this, 0, 0, localization)
            langButton.action = Runnable {
                //Convinience hack: Have ZSQD mapped ( WASD on azerty ) when french is used as a game language
                if (!allowBackButton && langButton.translationCode.endsWith("fr")) {
                    (gui.client.configuration.get<Configuration.Option<*>>("client.input.bind.forward") as Configuration.OptionInt).trySetting(GLFW.GLFW_KEY_Z)
                    (gui.client.configuration.get<Configuration.Option<*>>("client.input.bind.left") as Configuration.OptionInt).trySetting(GLFW.GLFW_KEY_Q)
                }

                (gui.client.configuration.get<Configuration.Option<*>>("client.game.language") as Configuration.OptionString).trySetting(langButton.translationCode)
                gui.client.content.localization().loadTranslation(langButton.translationCode)
                gui.popTopLayer()
            }

            elements.add(langButton)
            languages.add(langButton)
        }
    }

    override fun render(drawer: GuiDrawer) {
        if (scroll < 0)
            scroll = 0

        this.parentLayer?.render(drawer)

        var posY = gui.viewportHeight - (64 + 32)

        drawer!!.drawStringWithShadow(
                drawer.fonts.getFont("LiberationSans-Regular", 22f), 8,
                gui.viewportHeight - 32, "Welcome - Bienvenue - Wilkomen - Etc", -1,
                Vector4f(1f))

        var remainingSpace = gui.viewportHeight / 96 - 2

        while (scroll + remainingSpace > languages.size)
            scroll--

        var skip = scroll
        for (langButton in languages) {
            if (skip-- > 0)
                continue
            if (remainingSpace-- <= 0)
                break

            langButton.width = 256
            langButton.setPosition(gui.viewportWidth / 2 - langButton.width / 2, posY)
            langButton.render(drawer)
            posY -= langButton.height + 4
        }

        if (allowBackButton) {
            backOption.setPosition(8, 8)
            backOption.render(drawer)
        }
    }

    inner class LanguageButton internal constructor(layer: Layer, x: Int, y: Int, internal var translationCode: String) : Button(layer, x, y, 0, "") {
        internal val translationName: String

        init {
            translationName = gui.client.content.getAsset("./lang/$translationCode/lang.info")!!.read().use {
                try {
                    val reader = BufferedReader(InputStreamReader(gui.client.content.getAsset("./lang/$translationCode/lang.info")!!.read(), StandardCharsets.UTF_8))
                    reader.readLine()
                } catch (e: IOException) {
                    "Failed to read localization name($e)"
                }
            }
        }

        override fun render(drawer: GuiDrawer) {
            this.height = 64

            val texture = if (isFocused || isMouseOver)
                "./textures/gui/scalableButtonOver.png"
            else
                "./textures/gui/scalableButton.png"

            drawer.drawBoxWithCorners(xPosition, yPosition, width, height, 8, texture)
            //TODO ObjectRenderer.renderTexturedRect(xPosition + 40 * 1, yPosition + 32 * 1, 64 * 1, 48 * 1, "./lang/" + translationCode + "/lang.png");
            drawer.drawStringWithShadow(drawer.fonts.getFont("LiberationSans-Regular", 11f),
                    xPosition + 64 + 16, yPosition + 32, translationName, -1, Vector4f(1f))
        }
    }

    override fun handleInput(input: Input): Boolean {
        if (input is MouseScroll) {
            if (input.amount() < 0)
                scroll++
            else
                scroll--
            return true
        }

        return super.handleInput(input)
    }
}
