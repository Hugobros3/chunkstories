//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config

import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.gui.elements.Button
import io.xol.chunkstories.api.gui.elements.LargeButtonWithIcon
import io.xol.chunkstories.api.input.Input
import io.xol.chunkstories.api.input.Mouse.MouseButton
import io.xol.chunkstories.api.util.configuration.Configuration
import io.xol.chunkstories.api.util.configuration.Configuration.*
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW.glfwGetKeyName
import java.util.*

class OptionsScreen(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val exitButton = LargeButtonWithIcon(this, "back")
    private val configTabs = ArrayList<ConfigTab>()

    private val tabsButtons = ArrayList<Button>()
    private var selectedConfigTab = 0

    private val clientConfiguration: Configuration

    internal abstract inner class ConfigButton(open val option: Option<*>) : Button(this@OptionsScreen, 0, 0, option.name) {
        var run: Runnable? = null

        fun setApplyAction(run: Runnable): ConfigButton {
            this.run = run
            return this
        }

        fun apply() {
            if (run != null)
                run!!.run()
        }

        init {
            this.height = 24
            this.width = 160
        }

        open fun updateText() {
            this.text = gui.localization().getLocalizedString(option.name) + " : " + option.value
        }

        abstract fun onClick(posx: Float, posy: Float, button: Int)
    }

    internal inner class ConfigButtonToggle(override val option: OptionBoolean) : ConfigButton(option) {
        override fun onClick(posx: Float, posy: Float, button: Int) {
            option.toggle()
        }
    }

    internal inner class ConfigButtonMultiChoice(override val option: Configuration.OptionMultiChoice) : ConfigButton(option) {

        var values: Array<String> = option.possibleChoices.toTypedArray()
        var cuVal = 0

        init {
            for (i in values.indices) {
                if (values[i] == option.value)
                    cuVal = i
            }
        }

        override fun onClick(posx: Float, posy: Float, button: Int) {
            if (button == 0)
                cuVal++
            else
                cuVal--
            if (cuVal < 0)
                cuVal = values.size - 1
            if (cuVal >= values.size)
                cuVal = 0
            option.trySetting(values[cuVal])
        }
    }

    internal inner class ConfigButtonMultiChoiceInt(override val option: Configuration.OptionMultiChoiceInt) : ConfigButton(option) {

        var possibleValues: List<Int> = option.possibleChoices
        var currentValueIndex = 0

        init {
            for (i in possibleValues.indices) {
                if (possibleValues[i] == option.value)
                    currentValueIndex = i
            }
        }

        override fun onClick(posx: Float, posy: Float, button: Int) {
            if (button == 0)
                currentValueIndex++
            else
                currentValueIndex--
            if (currentValueIndex < 0)
                currentValueIndex = possibleValues.size - 1
            if (currentValueIndex >= possibleValues.size)
                currentValueIndex = 0
            option.trySetting(possibleValues[currentValueIndex])
        }

    }


    internal inner class ConfigButtonKey internal constructor(override val option: OptionKeyBind) : ConfigButton(option) {

        override fun updateText() {
            this.text = gui.localization().getLocalizedString(option.name) + " : " + glfwGetKeyName(option.value, 0)// Keyboard.getKeyName(Integer.parseInt(value));
        }

        override fun onClick(posx: Float, posy: Float, button: Int) {
            gui.topLayer = KeyBindSelectionOverlay(gui, this@OptionsScreen, this)
        }

        //TODO use a lambda
        internal fun callBack(key: Int) {
            option.trySetting(key)
            apply()
        }
    }

    internal inner class ConfigButtonScale(override val option: Configuration.OptionDoubleRange) : ConfigButton(option) {

        override fun onClick(mouseX: Float, mouseY: Float, button: Int) {
            val relativeMouseXPosition = (mouseX - this.positionX).toDouble()
            //System.out.println(relativeMouseXPosition);
            var newValue = (0.0 + Math.min(320.0, Math.max(0.0, relativeMouseXPosition))) / 160.0f

            newValue *= option.maximumValue - option.minimumValue
            newValue += option.minimumValue

            option.trySetting(newValue)
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

            //TODO redo call with modern api
            /*
			ObjectRenderer.renderTexturedRect(
					xPosition + this.width * scale() * (float) (option.getDoubleValue() - option.getMinimumValue())
							/ (float) (option.getMaximumValue() - option.getMinimumValue()),
					yPosition + 12 * scale(), 32 * scale(), 32 * scale(), 0, 0, 32, 32, 32,
					"./textures/gui/barCursor.png");*/

            drawer.drawStringWithShadow(drawer.fonts.defaultFont(), positionX + textStartOffset, positionY + 4, localizedText, -1, Vector4f(1.0f))
        }
    }

    internal inner class ConfigTab(var name: String) {
        var configButtons: MutableList<ConfigButton> = ArrayList()

        constructor(name: String, buttons: Array<ConfigButton>) : this(name) {
            for (b in buttons)
                configButtons.add(b)
        }
    }

    init {

        exitButton.action = Runnable {
            gui.client.configuration.save()
            gui.topLayer = parentLayer
        }
        elements.add(exitButton)

        clientConfiguration = gui.client.configuration

        for (option in clientConfiguration.options) {
            val name = option.name
            var category = name.substring("client.".length)

            category = category.substring(0, category.indexOf("."))
            category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase()

            val optionButton: ConfigButton

            if (option is Configuration.OptionMultiChoice)
                optionButton = ConfigButtonMultiChoice(option)
            else if (option is Configuration.OptionDoubleRange)
                optionButton = ConfigButtonScale(option)
            else if (option is OptionBoolean)
                optionButton = ConfigButtonToggle(option)
            else if (option is Configuration.OptionMultiChoiceInt)
                optionButton = ConfigButtonMultiChoiceInt(option)
            else if (option is Configuration.OptionKeyBind) {
                if (!option.hidden)
                    optionButton = ConfigButtonKey(option)
                else
                    continue
            } else
                continue

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

            relevantTab.configButtons.add(optionButton)
        }

        var configTabIndex = 0
        for (tab in configTabs) {
            // Add all these elements to the Gui handler
            elements.addAll(tab.configButtons)

            val tabButton = Button(this, 0, 0, tab.name)

            // Make the action of the tab buttons switching tab effectively
            val configTabIndex2 = configTabIndex
            tabButton.action = Runnable { selectedConfigTab = configTabIndex2 }

            configTabIndex++

            tabsButtons.add(tabButton)
            elements.add(tabButton)
        }
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
            dekal += (button.width / 2f).toInt()
            dekal += (button.width / 2f).toInt()
        }

        // Display the current tab
        val currentConfigTab = configTabs[selectedConfigTab]
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
            if (c is ConfigButtonScale && c.isMouseOver && mouse.mainButton.isPressed) {
                c.onClick(mouse.cursorX.toFloat(), mouse.cursorY.toFloat(), 0)
                c.apply()
            }

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
        } else if (input is MouseButton) {
            for (b in configTabs[selectedConfigTab].configButtons) {
                if (b.isMouseOver) {
                    b.onClick(input.mouse.cursorX.toFloat(), input.mouse.cursorY.toFloat(), if (input.name == "mouse.left") 0 else 1)
                    b.apply()
                }
            }
        }

        super.handleInput(input)
        return true
    }
}
