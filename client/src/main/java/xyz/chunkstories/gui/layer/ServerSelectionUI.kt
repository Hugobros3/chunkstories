//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.InputText
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.gui.elements.ScrollableContainer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.gui.layer.ServerSelectionUI.ServerSelectionZone.ServerGuiItem
import org.joml.Vector4f
import org.slf4j.LoggerFactory
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.client.ingame.connectToRemoteWorld
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket

class ServerSelectionUI internal constructor(gui: Gui, parent: Layer, private val automaticLogin: Boolean) : Layer(gui, parent) {
    private val backOption = LargeButtonWithIcon(this, "back")
    private val serverAddress = InputText(this, 0, 0, 250)
    private val connectButton = Button(this, 0, 0, 0, "#{connection.connect}")
    private val serverSelectionZone = ServerSelectionZone(this)
    private var movedInList = false

    private var currentServer = 0

    init {
        elements.add(serverAddress)

        focusedElement = serverAddress

        this.connectButton.action = Runnable { login() }
        this.backOption.action = Runnable { gui.topLayer = parentLayer }

        elements.add(connectButton)
        elements.add(serverSelectionZone)
        elements.add(backOption)

        val lastServer = gui.client.configuration.getValue(InternalClientOptions.lastServer)
        if (lastServer != "")
            serverAddress.text = lastServer

        refreshServers()
    }

    override fun render(drawer: GuiDrawer) {
        if (parentLayer != null) {
            //parentLayer.render(drawer);
        }

        width = gui.viewportWidth
        height = gui.viewportHeight

        if (automaticLogin && serverAddress.text != "")
            login()

        var posY = gui.viewportHeight
        posY -= 24 + 4

        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 18f)

        val instructions = "Select a server from the list or type in the address directly"
        drawer.drawStringWithShadow(titleFont, 8, posY, instructions, -1, Vector4f(1f))

        // gui
        val ipTextboxSize = gui.viewportWidth - connectButton.width - 8 - 8
        serverAddress.setPosition(8, gui.viewportHeight - 32 - 32)
        serverAddress.width = ipTextboxSize
        serverAddress.render(drawer)

        connectButton.setPosition(serverAddress.positionX + serverAddress.width + 4, gui.viewportHeight - 32 - 32)
        connectButton.render(drawer)

        backOption.setPosition(8, 8)
        backOption.render(drawer)

        updateServers()

        val offsetForButtons = backOption.positionY + backOption.height + 8
        val offsetForHeaderText = 8 + 32 + 32
        serverSelectionZone.setPosition((width - 480) / 2, offsetForButtons)
        serverSelectionZone.setSize(480, height - (offsetForButtons + offsetForHeaderText))
        serverSelectionZone.render(drawer)
    }

    override fun handleInput(input: Input): Boolean {
        when {
            input.name == "enter" -> login()
            input.name == "refreshServers" -> refreshServers()
            input.name == "repingServers" -> repingServers()
            input.name == "exit" -> gui.popTopLayer()
            serverSelectionZone.isFocused && input.name == "uiUp" -> {
                movedInList = true
                currentServer--
            }
            serverSelectionZone.isFocused && input.name == "uiDown" -> {
                movedInList = true
                currentServer++
            }
            else -> return super.handleInput(input)
        }

        return true
    }

    // Takes care of connecting to a server
    private fun login() {
        var serverAddress = serverAddress.text
        var port = 30410

        if (serverAddress.isEmpty())
            return

        if (serverAddress.contains(":")) {
            port = Integer.parseInt(serverAddress.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            serverAddress = serverAddress.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        val lastServerOption: Configuration.OptionString = gui.client.configuration[InternalClientOptions.lastServer]!!
        lastServerOption.trySetting("$serverAddress:$port")
        gui.client.configuration.save()

        // Launch the connection sequence
        (gui.client as ClientImplementation).connectToRemoteWorld(serverAddress, port)
    }

    private fun updateServers() {
        if (serverSelectionZone.elements.size == 0)
            return

        if (currentServer < 0)
            currentServer = 0
        if (currentServer > serverSelectionZone.elements.size - 1)
            currentServer = serverSelectionZone.elements.size - 1

        if (movedInList) {
            movedInList = false
            serverAddress.text = (serverSelectionZone.elements[currentServer] as ServerGuiItem).ip + if ((serverSelectionZone.elements[currentServer] as ServerGuiItem).port == 30410)
                ""
            else
                (serverSelectionZone.elements[currentServer] as ServerGuiItem).port
        }
    }

    private fun refreshServers() {
        TODO()
        /*SimpleWebRequest("https://chunkstories.xyz/api/listServers.php") { result ->
            serverSelectionZone.elements.clear()
            try {
                for (line in result.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    val address = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
                    serverSelectionZone.elements.add(serverSelectionZone.ServerGuiItem(address, 30410))
                }
            } catch (ignored: Exception) {
            }

        }*/
    }

    private fun repingServers() {
        for (ce in serverSelectionZone.elements) {
            try {
                (ce as ServerGuiItem).reload()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    internal inner class ServerSelectionZone(layer: Layer) : ScrollableContainer(layer) {

        internal inner class ServerGuiItem(var ip: String, var port: Int) : ContainerElement("Loading server info for $ip:$port", "") {

            var sd: ServerDataLoader? = null

            init {
                this.sd = ServerDataLoader(this, ip, port)
                //this.iconTextureLocation = "./cache/server-icon-$ip-$port.png"
            }

            override fun handleClick(mouseButton: MouseButton): Boolean {
                if (sd != null && sd!!.infoLoaded) {
                    serverAddress.text = sd!!.ip + if (sd!!.port == 30410) "" else sd!!.port
                    login()
                }

                return true
            }

            fun reload() {
                if (sd!!.infoError || sd!!.infoLoaded)
                    this.sd = ServerDataLoader(this, ip, port)
            }
        }
    }

    // Sub-class for server data loading
    inner class ServerDataLoader internal constructor(internal var parent: ServerGuiItem, internal var ip: String, internal var port: Int) : Thread() {
        internal var name = "Loading..."
        internal var description = "Loading..."
        internal var gameMode = "Loading..."
        internal var version = "Loading..."
        internal var infoLoaded = false
        internal var infoError = false
        internal var connectStart: Long = 0
        internal var ping: Long = 42

        init {
            this.setName("ServerData updater $ip$port")
            this.start()
        }

        override fun run() {
            try {
                connectStart = System.currentTimeMillis()
                val socket = Socket(ip, port)
                val `in` = DataInputStream(socket.getInputStream())
                val out = DataOutputStream(socket.getOutputStream())
                out.write(0x00.toByte().toInt())
                out.writeInt("info".toByteArray(charset("UTF-8")).size + 2)
                out.writeUTF("info")
                out.flush()

                ping = System.currentTimeMillis() - connectStart
                var lineRead = ""

                while (!lineRead.startsWith("info/done")) {
                    // Discard first byte, assummed to be packed id
                    `in`.readByte()
                    // Discard one more byte, assumed to be packet length
                    `in`.readInt()
                    lineRead = `in`.readUTF()
                    // System.out.println("red:"+lineRead);
                    if (lineRead.startsWith("info/")) {
                        val data = lineRead.replace("info/", "").split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (data[0] == "name")
                            name = data[1]
                        if (data[0] == "version")
                            version = data[1]
                        if (data[0] == "motd")
                            description = data[1]
                        if (data[0] == "connected")
                            gameMode = data[1] + " / " + data[2]
                    }
                }

                // Requests icon file
                out.write(0x00.toByte().toInt())
                out.writeInt("icon-file".toByteArray(charset("UTF-8")).size + 2)
                out.writeUTF("icon-file")
                out.flush()
                // Expect reply immediately
                val expect = `in`.readByte()
                logger.info("Expected:$expect")
                // Read and discard tag, we know what we are expecting
                `in`.readUTF()
                val fileLength = `in`.readLong()
                logger.info("fileLength:$fileLength")

                if (fileLength > 0) {
                    val file = File(
                            "./cache/server-icon-$ip-$port.png")
                    val fos = FileOutputStream(file)
                    var remaining = fileLength
                    val buffer = ByteArray(4096)
                    while (remaining > 0) {
                        val toRead = Math.min(4096, remaining)
                        val actuallyRead = `in`.read(buffer, 0, toRead.toInt())
                        fos.write(buffer, 0, actuallyRead)
                        remaining -= actuallyRead.toLong()
                    }
                    fos.close()
                }

                infoLoaded = true
                `in`.close()
                out.close()
                socket.close()
            } catch (e: Exception) {
                // e.printStackTrace();
                description = e.toString()
                gameMode = "Couldn't update."
                version = "Unkwnow version"

                infoError = true
                infoLoaded = true
            }

            parent.name = name
            parent.descriptionLines = "$description\n $gameMode"
            parent.topRightString = version
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("gui.serverselection")
    }
}
