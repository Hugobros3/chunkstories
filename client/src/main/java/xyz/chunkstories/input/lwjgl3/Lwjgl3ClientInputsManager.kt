//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import xyz.chunkstories.api.client.ClientInputsManager
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.events.client.ClientInputPressedEvent
import xyz.chunkstories.api.events.client.ClientInputReleasedEvent
import xyz.chunkstories.api.events.player.PlayerInputPressedEvent
import xyz.chunkstories.api.events.player.PlayerInputReleasedEvent
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.gui.layer.config.KeyBindSelectionUI
import xyz.chunkstories.net.packets.PacketInput
import xyz.chunkstories.world.WorldClientRemote
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWCharCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.glfw.GLFWMouseButtonCallback
import org.lwjgl.glfw.GLFWScrollCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.GameContext
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.input.*

class Lwjgl3ClientInputsManager(val gameWindow: GLFWWindow) : CommonInputsManager(), ClientInputsManager, /*InputsManagerLoader, */Cleanable {
    private val gui: Gui

    override val context: GameContext
        get() = gameWindow.client

    override val mouse: Lwjgl3Mouse

    private val keyCallback: GLFWKeyCallback
    private val mouseButtonCallback: GLFWMouseButtonCallback
    private val scrollCallback: GLFWScrollCallback
    private val characterCallback: GLFWCharCallback

    init {
        this.gui = gameWindow.client.gui

        mouse = Lwjgl3Mouse(this)

        keyCallback = object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (gui.topLayer is KeyBindSelectionUI) {
                    val kbs = gui.topLayer as KeyBindSelectionUI?
                    kbs!!.setKeyTo(key)
                }

                // Try first the compound shortcuts
                val keyBindCompound = getKeyCompoundFulForLWJGL3xKey(key)
                if (keyBindCompound != null) {
                    if (action == GLFW_PRESS)
                        if (onInputPressed(keyBindCompound))
                            return
                }

                // If unsuccessfull pass to normal keyboard input
                val keyboardInput = getKeyBoundForLWJGL3xKey(key)

                if (keyboardInput != null) {
                    if (action == GLFW_PRESS)
                        onInputPressed(keyboardInput)
                    else if (action == GLFW_REPEAT && keyboardInput.repeat)
                        onInputPressed(keyboardInput)
                    else if (action == GLFW_RELEASE)
                        onInputReleased(keyboardInput)
                }

                // Unhandled character
            }
        }
        glfwSetKeyCallback(gameWindow.glfwWindowHandle, keyCallback)

        mouseButtonCallback = object : GLFWMouseButtonCallback() {
            override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
                var mButton: MouseButton? = null
                when (button) {
                    0 -> mButton = mouse.mainButton
                    1 -> mButton = mouse.secondaryButton
                    2 -> mButton = mouse.middleButton
                }

                if (mButton != null) {
                    if (action == GLFW_PRESS)
                        onInputPressed(mButton)
                    else if (action == GLFW_RELEASE)
                        onInputReleased(mButton)
                }
            }

        }
        glfwSetMouseButtonCallback(gameWindow.glfwWindowHandle, mouseButtonCallback)

        scrollCallback = object : GLFWScrollCallback() {
            override fun invoke(window: Long, xoffset: Double, yoffset: Double) {

                onInputPressed(mouse.generalMouseScrollEvent(yoffset))

                // gameWindow.getCurrentScene().onScroll((int)yoffset);
            }
        }
        glfwSetScrollCallback(gameWindow.glfwWindowHandle, scrollCallback)

        characterCallback = object : GLFWCharCallback() {

            override fun invoke(window: Long, codepoint: Int) {
                val chars = Character.toChars(codepoint)
                for (c in chars) {
                    val layer = gui.topLayer
                    layer?.handleTextInput(c)
                }
            }

        }
        glfwSetCharCallback(gameWindow.glfwWindowHandle, characterCallback)

        //reload();
    }

    /**
     * Returns null or a KeyBind matching the pressed key
     */
    private fun getKeyBoundForLWJGL3xKey(keyCode: Int): Lwjgl3KeyBind? {
        for (keyBind in allInputs) {
            if (keyBind is Lwjgl3KeyBind && keyBind.lwjglKey == keyCode)
                return keyBind
        }
        return null
    }

    private fun getKeyCompoundFulForLWJGL3xKey(key: Int): Lwjgl3KeyBindCompound? {
        inputs@ for (keyBind in allInputs) {
            if (keyBind is Lwjgl3KeyBindCompound) {

                // Check all other keys were pressed
                for (glfwKey in keyBind.glfwKeys) {
                    if (glfwGetKey(gameWindow.glfwWindowHandle, glfwKey) != GLFW_PRESS)
                        continue@inputs
                }

                return keyBind
            }
        }

        return null
    }

    override fun addBuiltInInputs(inputs: MutableList<AbstractInput>) {
        // Add physical mouse buttons
        inputs.add(mouse.mainButton)
        inputs.add(mouse.secondaryButton)
        inputs.add(mouse.middleButton)
    }

    override fun insertInput(inputs: MutableList<AbstractInput>, inputType: InputType, inputName: String, defaultValue: String?, arguments: MutableList<String>) {
        inputs.add(when(inputType) {
            InputType.KEY_BIND -> Lwjgl3KeyBind(this, inputName, defaultValue!!, arguments.contains("hidden"), arguments.contains("repeat"))
            InputType.KEY_COMPOUND ->Lwjgl3KeyBindCompound(this, inputName, defaultValue!!)
            InputType.VIRTUAL -> InputVirtual(this, inputName)
        })
    }

    fun updateInputs() {
        for (input in allInputs) {
            if (input is Pollable)
                input.updateStatus()
        }
    }

    override fun onInputPressed(input: Input): Boolean {
        if (input.name == "fullscreen") {
            //TODO
            //gameWindow.toggleFullscreen();
            return true
        }

        val ingameClient = gameWindow.client.ingame

        // Try the client-side event press
        val inputPressedEvent = ClientInputPressedEvent(gameWindow.client, input)
        ingameClient?.pluginManager?.fireEvent(inputPressedEvent)
        if (inputPressedEvent.isCancelled)
            return false

        // Try the GUI handling
        val layer = gameWindow.client.gui.topLayer
        if (layer?.handleInput(gameWindow.client.gui.translateInputForGui(input)) == true)
            return true

        if (ingameClient == null)
            return false

        val player = ingameClient.player
        val playerEntity = player.controlledEntity ?: return false

        // There has to be a controlled entity for sending inputs to make sense.

        val world = playerEntity.world

        // Send input to server
        if (world is WorldClientRemote) {
            // MouseScroll inputs are strictly client-side
            if (input !is Mouse.MouseScroll) {
                val connection = (playerEntity.world as WorldClientRemote).connection
                val packet = PacketInput(world)
                packet.input = input
                packet.isPressed = true
                connection.pushPacket(packet)
            }

            return playerEntity.traits.tryWithBoolean(TraitControllable::class) { this.onControllerInput(input) }
        } else {
            val playerInputPressedEvent = PlayerInputPressedEvent(player, input)
            ingameClient.pluginManager.fireEvent(playerInputPressedEvent)

            if (playerInputPressedEvent.isCancelled)
                return false
        }

        // Handle interaction locally
        return playerEntity.traits.tryWithBoolean(TraitControllable::class) { this.onControllerInput(input) }
    }

    override fun onInputReleased(input: Input): Boolean {
        val ingameClient = gameWindow.client.ingame ?: return false

        val event = ClientInputReleasedEvent(gameWindow.client, input)
        ingameClient.pluginManager.fireEvent(event)

        val player = ingameClient.player
        val entityControlled = player.controlledEntity ?: return false

        // There has to be a controlled entity for sending inputs to make sense.

        // Send input to server
        val world = entityControlled.world
        return if (world is WorldClientRemote) {
            val connection = (entityControlled.world as WorldClientRemote).connection
            val packet = PacketInput(world)
            packet.input = input
            packet.isPressed = false
            connection.pushPacket(packet)
            true
        } else {
            val event2 = PlayerInputReleasedEvent(player, input!!)
            ingameClient.pluginManager.fireEvent(event2)
            true
        }
    }

    override fun cleanup() {
        this.keyCallback.free()
        this.mouseButtonCallback.free()
        this.scrollCallback.free()
        this.characterCallback.free()
    }

    fun logger(): Logger {
        return logger
    }

    companion object {
        private val logger = LoggerFactory.getLogger("client.workers")
    }
}
