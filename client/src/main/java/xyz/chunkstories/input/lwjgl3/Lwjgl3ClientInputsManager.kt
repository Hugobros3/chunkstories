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
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.gui.layer.config.KeyBindSelectionOverlay
import xyz.chunkstories.input.InputVirtual
import xyz.chunkstories.input.InputsLoaderHelper
import xyz.chunkstories.input.InputsManagerLoader
import xyz.chunkstories.input.Pollable
import xyz.chunkstories.net.packets.PacketInput
import xyz.chunkstories.world.WorldClientRemote
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWCharCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.glfw.GLFWMouseButtonCallback
import org.lwjgl.glfw.GLFWScrollCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.graphics.vulkan.resources.Cleanable

class Lwjgl3ClientInputsManager// private final IngameLayer scene;
(val gameWindow: GLFWWindow) : ClientInputsManager, InputsManagerLoader, Cleanable {
    private val gui: Gui

    internal var inputs = mutableListOf<Input>()
    internal var inputsMap: MutableMap<Long, Input> = mutableMapOf()

    var mouse: Lwjgl3Mouse
    var LEFT: Lwjgl3MouseButton
    var RIGHT: Lwjgl3MouseButton
    var MIDDLE: Lwjgl3MouseButton

    private val keyCallback: GLFWKeyCallback
    private val mouseButtonCallback: GLFWMouseButtonCallback
    private val scrollCallback: GLFWScrollCallback
    private val characterCallback: GLFWCharCallback

    init {
        this.gui = gameWindow.client.gui

        mouse = Lwjgl3Mouse(this)
        LEFT = Lwjgl3MouseButton(mouse, "mouse.left", 0)
        RIGHT = Lwjgl3MouseButton(mouse, "mouse.right", 1)
        MIDDLE = Lwjgl3MouseButton(mouse, "mouse.middle", 2)

        keyCallback = object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (gui.topLayer is KeyBindSelectionOverlay) {
                    val kbs = gui.topLayer as KeyBindSelectionOverlay?
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
                    0 -> mButton = LEFT
                    1 -> mButton = RIGHT
                    2 -> mButton = MIDDLE
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

                val ms = mouse.scroll(yoffset)
                onInputPressed(ms)

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

    override fun getAllInputs(): Iterator<Input> {
        return inputs.iterator()
    }

    /**
     * Returns null or a KeyBind matching the name
     */
    override fun getInputByName(bindName: String): Input? {
        if (bindName == "mouse.left")
            return LEFT
        if (bindName == "mouse.right")
            return RIGHT
        if (bindName == "mouse.middle")
            return MIDDLE

        //TODO hash map !!!
        for (keyBind in inputs) {
            if (keyBind.name == bindName)
                return keyBind
        }
        return null
    }

    /**
     * Returns null or a KeyBind matching the pressed key
     *
     * @param keyCode
     * @return
     */
    protected fun getKeyBoundForLWJGL3xKey(keyCode: Int): Lwjgl3KeyBind? {
        for (keyBind in inputs) {
            if (keyBind is Lwjgl3KeyBind && keyBind.lwjglKey == keyCode)
                return keyBind
        }
        return null
    }

    protected fun getKeyCompoundFulForLWJGL3xKey(key: Int): Lwjgl3KeyBindCompound? {
        inputs@ for (keyBind in inputs) {
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

    override fun getInputFromHash(hash: Long): Input? {
        if (hash == 0L)
            return LEFT
        else if (hash == 1L)
            return RIGHT
        else if (hash == 2L)
            return MIDDLE

        return inputsMap[hash]
    }

    fun reload() {
        inputs.clear()
        inputsMap.clear()

        InputsLoaderHelper.loadKeyBindsIntoManager(this, gameWindow.client.content.modsManager())

        // Add physical mouse buttons
        inputs.add(LEFT)
        inputsMap[LEFT.hash] = LEFT
        inputs.add(RIGHT)
        inputsMap[RIGHT.hash] = RIGHT
        inputs.add(MIDDLE)
        inputsMap[MIDDLE.hash] = MIDDLE
    }

    override fun insertInput(type: String, name: String, value: String?, arguments: Collection<String>) {
        val input: Input
        when (type) {
            "keyBind" -> input = Lwjgl3KeyBind(this, name, value!!, arguments.contains("hidden"), arguments.contains("repeat"))
            "virtual" -> input = InputVirtual(name)
            "keyBindCompound" -> {
                val keyCompound = Lwjgl3KeyBindCompound(this, name, value)
                input = keyCompound
            }
            else -> return
        }

        inputs.add(input)
        inputsMap[input.hash] = input
    }

    fun updateInputs() {
        for (input in this.inputs) {
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
        if (layer?.handleInput(input) == true)
            return true

        if (ingameClient == null)
            return false

        val player = ingameClient.player
        val playerEntity = player.controlledEntity ?: return false

        // There has to be a controlled entity for sending inputs to make sense.

        val world = playerEntity.getWorld()

        // Send input to server
        if (world is WorldClientRemote) {
            // MouseScroll inputs are strictly client-side
            if (input !is MouseScroll) {
                val connection = (playerEntity.getWorld() as WorldClientRemote).connection
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

    override fun onInputReleased(input: Input?): Boolean {
        val ingameClient = gameWindow.client.ingame ?: return false

        val event = ClientInputReleasedEvent(gameWindow.client, input)
        ingameClient.pluginManager.fireEvent(event)

        val player = ingameClient.player
        val entityControlled = player.controlledEntity ?: return false

        // There has to be a controlled entity for sending inputs to make sense.

        // Send input to server
        val world = entityControlled.getWorld()
        if (world is WorldClientRemote) {
            val connection = (entityControlled.getWorld() as WorldClientRemote).connection
            val packet = PacketInput(world)
            packet.input = input
            packet.isPressed = false
            connection.pushPacket(packet)
            return true
        } else {
            val event2 = PlayerInputReleasedEvent(player, input)
            ingameClient.pluginManager.fireEvent(event2)
            return true
        }
    }

    override fun getMouse(): Mouse {
        return mouse
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
