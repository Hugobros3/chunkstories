//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.config

import org.joml.Vector4f
import org.lwjgl.glfw.GLFW.glfwGetKeyName
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.api.util.configuration.Configuration.*
import xyz.chunkstories.input.lwjgl3.GLFWKeyIndexHelper
import xyz.chunkstories.input.lwjgl3.Lwjgl3MouseButton
import java.util.*

class OptionsScreen(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val exitButton = LargeButtonWithIcon(this, "back")
    private val configTabs = ArrayList<ConfigTab>()

    private val tabsButtons = ArrayList<Button>()
    private var selectedConfigTabIndex = 0

    private val clientConfiguration: Configuration

    internal inner class ConfigTab(val name: String) {
        val configButtons: MutableList<ConfigButton> = ArrayList()
    }

    internal abstract inner class ConfigButton(open val option: Option<*>, val tab: ConfigTab) : Button(this@OptionsScreen, 0, 0, option.name) {
        init {
            this.height = 24
            this.width = 160
        }

        open fun updateText() {
            this.text = gui.localization().getLocalizedString(option.name) + " : " + option.value
        }

        //abstract fun onClick(posx: Float, posy: Float, button: Int)
    }

    internal inner class ConfigButtonToggle(override val option: OptionBoolean, tab: ConfigTab) : ConfigButton(option, tab) {

        override fun handleClick(mouseButton: MouseButton): Boolean {
            option.toggle()
            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg")
            return true
        }
    }

    internal inner class ConfigButtonMultiChoice(override val option: Configuration.OptionMultiChoice, tab: ConfigTab) : ConfigButton(option, tab) {

        var values: Array<String> = option.possibleChoices.toTypedArray()
        var cuVal = 0

        init {
            for (i in values.indices) {
                if (values[i] == option.value)
                    cuVal = i
            }
        }

        override fun handleClick(mouseButton: MouseButton): Boolean {
            if (mouseButton == gui.mouse.mainButton)
                cuVal++
            else
                cuVal--

            if (cuVal < 0)
                cuVal = values.size - 1
            if (cuVal >= values.size)
                cuVal = 0
            option.trySetting(values[cuVal])

            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg", SoundSource.Mode.NORMAL, null, if(mouseButton == gui.mouse.mainButton) 1f else 0.8f, 1f)
            return true
        }
    }

    internal inner class ConfigButtonMultiChoiceInt(override val option: Configuration.OptionMultiChoiceInt, tab: ConfigTab) : ConfigButton(option, tab) {

        var possibleValues: List<Int> = option.possibleChoices
        var currentValueIndex = 0

        init {
            for (i in possibleValues.indices) {
                if (possibleValues[i] == option.value)
                    currentValueIndex = i
            }
        }

        override fun handleClick(mouseButton: MouseButton): Boolean {
            if (mouseButton == gui.mouse.mainButton)
                currentValueIndex++
            else
                currentValueIndex--

            if (currentValueIndex < 0)
                currentValueIndex = possibleValues.size - 1
            if (currentValueIndex >= possibleValues.size)
                currentValueIndex = 0
            option.trySetting(possibleValues[currentValueIndex])

            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg", SoundSource.Mode.NORMAL, null, if(mouseButton == gui.mouse.mainButton) 1f else 0.8f, 1f)
            return true
        }

    }

    internal inner class ConfigButtonKey internal constructor(override val option: OptionKeyBind, tab: ConfigTab) : ConfigButton(option, tab) {

        override fun updateText() {
            this.text = gui.localization().getLocalizedString(option.name) + ": " + (GLFWKeyIndexHelper.shortNames[option.value] ?: "?").toUpperCase()
                    //glfwGetKeyName(option.value, 0)// Keyboard.getKeyName(Integer.parseInt(value));
        }

        override fun handleClick(mouseButton: MouseButton): Boolean {
            gui.topLayer = KeyBindSelectionOverlay(gui, this@OptionsScreen, this)
            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg")
            return true
        }

        //TODO use a lambda
        internal fun callBack(key: Int) {
            option.trySetting(key)
        }
    }

    internal inner class ConfigButtonScale(override val option: Configuration.OptionDoubleRange, tab: ConfigTab) : ConfigButton(option, tab) {

        override fun handleClick(mouseButton: MouseButton): Boolean {
            drag()
            this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg")
            return true
        }

        fun drag() {
            val oldValue = option.value

            val mouseX = gui.mouse.cursorX
            val relativeMouseXPosition = (mouseX - this.positionX)
            //System.out.println(relativeMouseXPosition);
            var newValue = (0.0 + Math.min(320.0, Math.max(0.0, relativeMouseXPosition))) / 160.0f

            newValue *= option.maximumValue - option.minimumValue
            newValue += option.minimumValue


            option.trySetting(newValue)

            val actualNewValue = option.value
            val normalizedValue = (actualNewValue - option.minimumValue) / (option.maximumValue - option.minimumValue)

            if(oldValue != actualNewValue) {
                this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_drag_slider.ogg", SoundSource.Mode.NORMAL, null, 0.5f + normalizedValue.toFloat(), 1f)
                //println("$oldValue != $actualNewValue $normalizedValue")
            }
        }

        override fun render(drawer: GuiDrawer) {
            val value = option.value

            this.text = gui.localization().getLocalizedString(option.name) + " : " + String.format("%." + 3 + "G", value)// Keyboard.getKeyName(Integer.parseInt(value));

            val localizedText = gui.localization().localize(text)
            val textWidth = gui.fonts.defaultFont().getWidth(localizedText)
            if (width < 0) {
                width = textWidth
            }
            val textStartOffset = width / 2 - textWidth / 2
            val texture = "textures/gui/scalableField.png"

            drawer.drawBoxWithCorners(positionX, positionY, width, height, 8, texture)

            val virtualMouse = gui.mouse
            if (this.isMouseOver && (virtualMouse.mainButton as Lwjgl3MouseButton).isDown) {
                drag()
            }

            val normalizedValue = (value - option.minimumValue) / (option.maximumValue - option.minimumValue)
            drawer.drawBox(positionX - 16 + (width * normalizedValue).toInt(), positionY - 4, 32, 32, "textures/gui/barCursor.png")
            drawer.drawStringWithShadow(drawer.fonts.defaultFont(), positionX + textStartOffset, positionY + 4, localizedText, -1, Vector4f(1.0f))
        }
    }

    init {

        exitButton.action = Runnable {
            gui.client.configuration.save()
            gui.topLayer = parentLayer
        }
        elements.add(exitButton)

        clientConfiguration = gui.client.configuration

        loop@ for (option in clientConfiguration.options) {
            val name = option.name
            var category = name.substring("client.".length)

            val i = category.indexOf(".")
            if(i < 0)
                continue
            category = category.substring(0, i)
            category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase()

            val optionButton: ConfigButton

            var relevantTab: ConfigTab? = null
            for (tab in configTabs) {
                if (tab.name == category) {
                    relevantTab = tab
                    break
                }
            }
            if (relevantTab == null) {
                relevantTab = ConfigTab(category)
                configTabs.add(0, relevantTab)
            }

            when (option) {
                is Configuration.OptionMultiChoice -> optionButton = ConfigButtonMultiChoice(option, relevantTab)
                is Configuration.OptionDoubleRange -> optionButton = ConfigButtonScale(option, relevantTab)
                is OptionBoolean -> optionButton = ConfigButtonToggle(option, relevantTab)
                is Configuration.OptionMultiChoiceInt -> optionButton = ConfigButtonMultiChoiceInt(option, relevantTab)
                is Configuration.OptionKeyBind -> if (!option.hidden) {
                    optionButton = ConfigButtonKey(option, relevantTab)
                } else
                    continue@loop
                else -> continue@loop
            }

            relevantTab.configButtons.add(optionButton)
        }

        for ((configTabIndex, tab) in configTabs.withIndex()) {
            // Add all these elements to the Gui handler
            val tabButton = Button(this, 0, 0, tab.name)

            // Capture the index for the lambda
            //val capturedIndex = configTabIndex

            // Clicking on a button removes the buttons from the previous tab, changes tab and adds it's buttons to the clickable elements
            tabButton.action = Runnable {
                val oldTab = configTabs[selectedConfigTabIndex]
                elements.removeAll(oldTab.configButtons)
                selectedConfigTabIndex = configTabIndex
                elements.addAll(tab.configButtons)
            }

            tabsButtons.add(tabButton)
            elements.add(tabButton)
        }

        elements.addAll(configTabs[selectedConfigTabIndex].configButtons)
    }

    override fun render(renderer: GuiDrawer) {
        parentLayer?.rootLayer?.render(renderer)

        val optionsPanelSize = 160 * 2 + 16 + 32

        // Shades the BG
        renderer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))
        renderer.drawBox(gui.viewportWidth / 2 - optionsPanelSize / 2, 0, optionsPanelSize, gui.viewportHeight,
                Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        // Render the tabs buttons
        var dekal = 16
        for (button in tabsButtons) {
            button.setPosition(gui.viewportWidth / 2 - optionsPanelSize / 2 + dekal, gui.viewportHeight - 64)
            button.render(renderer)

            dekal += button.width + 2
        }

        // Display the current tab
        val currentConfigTab = configTabs[selectedConfigTabIndex]
        var a = 0
        var b = 0
        val startPosX = gui.viewportWidth / 2 - optionsPanelSize / 2 + 16
        val startPosY = gui.viewportHeight - (64 + 32)

        val mouse = gui.mouse

        for (c in currentConfigTab.configButtons) {
            c.setPosition(startPosX + b * (160 + 16), startPosY - a / 2 * 32)
            c.updateText()
            c.render(renderer)

            // Scale buttons work a bit hackyshly
            /*if (c is ConfigButtonScale && c.isMouseOver && mouse.mainButton.isPressed) {
                c.onClick(mouse.cursorX.toFloat(), mouse.cursorY.toFloat(), 0)
            }*/

            a++
            b = a % 2
        }

        renderer.drawStringWithShadow(renderer.fonts.getFont("LiberationSans-Regular", 11f), gui.viewportWidth / 2 - optionsPanelSize / 2 + 16,
                gui.viewportHeight - 32, gui.localization().getLocalizedString("options.title"), -1, Vector4f(1f))

        exitButton.setPosition(8, 8)
        exitButton.render(renderer)
    }

    override fun handleInput(input: Input): Boolean {
        if (input.name == "exit") {
            clientConfiguration.save()
            gui.popTopLayer()
            return true
        }/* else if (input is MouseButton) {
            for (b in configTabs[selectedConfigTabIndex].configButtons) {
                if (b.isMouseOver) {
                    val virtualMouse = gui.mouse
                    b.onClick(virtualMouse.cursorX.toFloat(), virtualMouse.cursorY.toFloat(), if (input.name == "mouse.left") 0 else 1)
                }
            }
        }*/

        super.handleInput(input)
        return true
    }
}
