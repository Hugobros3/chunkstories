//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.ingame

import java.util.ArrayList
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.client.InternalClientOptions
import org.joml.Vector4f

import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.InputText
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.api.plugin.ChunkStoriesPlugin
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.world.WorldClientLocal
import xyz.chunkstories.world.WorldClientRemote

class ChatManager(private val ingameClient: IngameClient, private val ingameGuiLayer: IngameLayer) {
    private val gui: Gui

    private val chatHistorySize = 150

    private val chat = ConcurrentLinkedDeque<ChatLine>()
    private val sent = ArrayList<String>()

    private var sentMessages = 0
    private var sentHistory = 0

    private var scroll = 0

    init {
        this.gui = ingameGuiLayer.gui
    }

    private inner class ChatLine(text: String?) {

        var time: Long = 0
        var text: String

        init {
            var text = text
            if (text == null)
                text = ""
            this.text = text
            time = System.currentTimeMillis()
        }

        fun clickRelative(x: Int, y: Int) {
            // TODO clickable text, urls etc
        }
    }

    inner class ChatLayer(gui: Gui, parent: Layer) : Layer(gui, parent) {
        internal var inputBox: InputText

        internal var delay: Long = 0

        init {

            // Add the inputBox
            this.inputBox = InputText(this, 0, 0, 500)
            this.elements.add(inputBox)
            this.setFocusedElement(inputBox)

            // Reset the scroll
            this@ChatManager.scroll = 0

            // 150ms of delay to avoid typing in by mistake
            this.delay = System.currentTimeMillis() + 30
        }

        override fun render(drawer: GuiDrawer?) {
            parentLayer!!.render(drawer)

            inputBox.setPosition(8, 48)
            inputBox.isTransparent = true
            inputBox.render(drawer!!)
        }

        override fun onResize(newWidth: Int, newHeight: Int) {
            parentLayer!!.onResize(newWidth, newHeight)
        }

        override fun handleInput(input: Input): Boolean {
            when {
                input.name == "exit" -> {
                    gui.popTopLayer()
                    return true
                }
                input.name == "uiUp" -> {
                    // sentHistory = 0 : empty message, = 1 last message, 2 last message before etc
                    if (sentMessages > sentHistory) {
                        sentHistory++
                    }
                    if (sentHistory > 0)
                        inputBox.text = sent[sentHistory - 1]
                    else
                        inputBox.text = ""
                }
                input.name == "uiDown" -> {
                    // sentHistory = 0 : empty message, = 1 last message, 2 last message before etc
                    if (sentHistory > 0) {
                        sentHistory--
                    }
                    if (sentHistory > 0)
                        inputBox.text = sent[sentHistory - 1]
                    else
                        inputBox.text = ""
                }
                input.name == "enter" -> {
                    processTextInput(inputBox.text)
                    inputBox.text = ""
                    sentHistory = 0
                    gui.popTopLayer()
                    return true
                }
                input is MouseScroll -> if (input.amount() > 0)
                    scroll++
                else
                    scroll--
            }
            inputBox.handleInput(input)

            return true
        }

        override fun handleTextInput(c: Char): Boolean {
            return if (System.currentTimeMillis() <= delay)
                false
            else
                super.handleTextInput(c)
        }
    }

    fun drawChatWindow(drawer: GuiDrawer) {
        while (chat.size > chatHistorySize)
            chat.removeLast()
        if (scroll < 0)
        // || !chatting)
            scroll = 0
        if (scroll > chatHistorySize)
            scroll = chatHistorySize
        var scrollLinesSkip = scroll
        var linesDrew = 0
        val maxLines = 18
        val i = chat.iterator()
        while (linesDrew < maxLines && i.hasNext()) {
            // if (a >= chatHistorySize - lines)
            val line = i.next()
            if (scrollLinesSkip > 0) {
                scrollLinesSkip--
                continue
            }

            val font = gui.fonts.getFont("LiberationSans-Regular__aa", 18f)

            val chatWidth = Math.max(750, gui.viewportWidth / 4 * 3)
            val localizedLine = gui.localization().localize(line.text)
            val actualLines = font.getLinesHeight(localizedLine, chatWidth.toFloat())
            linesDrew += actualLines
            var textFade = (line.time + 10000L - System.currentTimeMillis()) / 1000f

            if (textFade > 0f || gui.topLayer is ChatLayer) {
                if (textFade > 1 || gui.topLayer is ChatLayer)
                    textFade = 1f

                drawer.drawStringWithShadow(font, 8, linesDrew * font.lineHeight + 64, localizedLine, chatWidth, Vector4f(1f, 1f, 1f, textFade))
            }
        }
    }

    fun insert(t: String) {
        chat.addFirst(ChatLine(t))
    }

    private fun processTextInput(input: String) {
        val clientUserName = ingameClient.user.name

        if (input.startsWith("/")) {
            var chatMsg = input

            chatMsg = chatMsg.substring(1, chatMsg.length)

            var cmdName = chatMsg.toLowerCase()
            var args = arrayOf<String>()
            if (chatMsg.contains(" ")) {
                cmdName = chatMsg.substring(0, chatMsg.indexOf(" "))
                args = chatMsg.substring(chatMsg.indexOf(" ") + 1, chatMsg.length).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            }

            when {
                ingameClient.pluginManager.dispatchCommand(ingameClient.player, cmdName, args) -> {
                    if (sent.size == 0 || sent[0] != input) {
                        sent.add(0, input)
                        sentMessages++
                    }
                    return
                }
                cmdName == "plugins" -> {
                    val count = ingameClient.pluginManager.activePlugins().count()
                    val list = ingameClient.pluginManager.activePlugins().map { it.name }.joinToString(", ")

                    if (ingameClient.world is WorldClientLocal)
                        insert("#00FFD0$count active client [master] plugins : $list")
                    else
                        insert("#74FFD0$count active client [remote] plugins : $list")

                    if (sent.size == 0 || sent[0] != input) {
                        sent.add(0, input)
                        sentMessages++
                    }
                }
                cmdName == "mods" -> {
                    var list = ""
                    var i = 0
                    for (mod in ingameClient.content.modsManager().currentlyLoadedMods) {
                        i++
                        list += mod.modInfo.name + if (i == ingameClient.content.modsManager().currentlyLoadedMods.size) "" else ", "
                    }

                    if (ingameClient.world is WorldClientLocal)
                        insert("#FF0000$i active client [master] mods : $list")
                    else
                        insert("#FF7070$i active client [remote] mods : $list")

                    if (sent.size == 0 || sent[0] != input) {
                        sent.add(0, input)
                        sentMessages++
                    }
                }
                cmdName == "buddydbg" -> {
                    val glfwWindow = ingameClient.gameWindow as GLFWWindow
                    val graphicsBackend = glfwWindow.graphicsEngine.backend as VulkanGraphicsBackend
                    graphicsBackend.memoryManager.debug()
                    insert("#FF7070FUCK")
                }
            }
        }

        if (input == "/locclear") {
            chat.clear()
        } else if (input == "I am Mr Debug") {
            // it was you this whole time
            val option = ingameClient.configuration.get<Configuration.OptionBoolean>(InternalClientOptions.debugMode)
            option!!.trySetting(true)
        }

        if (ingameClient.world is WorldClientRemote)
            (ingameClient.world as WorldClientRemote).connection
                    .sendTextMessage("chat/$input")
        else
            insert(ColorsTools.getUniqueColorPrefix(clientUserName) + clientUserName + "#FFFFFF > " + input)

        println("$clientUserName > $input")

        if (sent.size == 0 || sent[0] != input) {
            sent.add(0, input)
            sentMessages++
        }
    }
}
