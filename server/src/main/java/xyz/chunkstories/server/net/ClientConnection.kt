//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.events.player.PlayerChatEvent
import xyz.chunkstories.api.events.player.PlayerLogoutEvent
import xyz.chunkstories.api.net.Interlocutor
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.content.translator.AbstractContentTranslator
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.PacketsEncoderDecoder
import xyz.chunkstories.net.packets.PacketContentTranslator
import xyz.chunkstories.net.packets.PacketSendFile
import xyz.chunkstories.net.packets.PacketSendWorldInfo
import xyz.chunkstories.server.net.ServerPacketsProcessorImplementation.ClientPacketsContext
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.world.WorldServer
import xyz.chunkstories.world.spawnPlayer

abstract class ClientConnection(val context: Server, internal val clientsManager: ClientsManager, remoteAddress: String, port: Int) : Connection(remoteAddress, port), Interlocutor {
    internal val logger: Logger

    private val loginHelper: PlayerAuthenticationHelper

    abstract override var encoderDecoder: ClientPacketsContext protected set
    protected var player: ServerPlayer? = null

    init {
        // This way we can tell the logs from one use to the next
        this.logger = LoggerFactory.getLogger("server.net.users." + usersCount.getAndIncrement())

        this.loginHelper = PlayerAuthenticationHelper(this)
    }

    override fun connect(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun handleSystemRequest(message: String): Boolean {
        var message = message
        if (message.startsWith("info")) {
            this.clientsManager.sendServerInfo(this)
            return true

        } else if (message.startsWith("login/")) {
            return loginHelper.handleLogin(message.substring(6, message.length))

        } else if (message == "mods") {
            sendTextMessage("info/mods:" + clientsManager.getServer().modsProvider.modsString)
            this.flush()
            return true

        } else if (message == "icon-file") {
            val iconPacket = PacketSendFile()
            iconPacket.file = File("server-icon.png")
            iconPacket.fileTag = "server-icon"
            this.pushPacket(iconPacket)
            this.flush()
            return true
        }

        // Any other commands need an authentificated player !
        val player = this.player ?: return false

        // Login-mandatory requests ( you need to be authentificated to use them )
        if (message == "co/off") {
            this.close("Client-terminated connection")

        } else if (message.startsWith("send-mod/")) {
            val modDescriptor = message.substring(9)

            val md5 = modDescriptor.substring(4)
            logger.info("$this asked to be sent mod $md5")

            // Give him what he asked for.
            val found = clientsManager.getServer().modsProvider.obtainModRedistribuable(md5)
            if (found == null) {
                logger.info("No such mod found.")
            } else {
                logger.info("Pushing mod md5 " + md5 + "to user.")
                val modUploadPacket = PacketSendFile()
                modUploadPacket.file = found
                modUploadPacket.fileTag = modDescriptor
                this.pushPacket(modUploadPacket)
            }

        } else if (message.startsWith("world/")) {
            // TODO this bit will obviously need to be rewritten when I get arround to doing
            // multiworld support
            val world = clientsManager.getServer().world
            message = message.substring(6, message.length)

            if (message == "enter") {
                player.whenEnteringWorld(world)
                // Sends the construction info for the world, and then the player entity
                val packet = PacketSendWorldInfo(world.worldInfo)
                pushPacket(packet)

                // TODO only spawn the player when he asks to
                world.spawnPlayer(player)
                return true

            } else if (message == "translator") {
                val packet = PacketContentTranslator(
                        world.contentTranslator)
                player.pushPacket(packet)
                return true

            } else if (message == "respawn") {
                // Only allow to respawn if the current entity is null or dead
                if (player.controlledEntity == null || player.controlledEntity?.traits?.get(TraitHealth::class)?.isDead == true) {
                    world.spawnPlayer(player)
                    player.sendMessage("Respawning ...")
                } else
                    player.sendMessage("You're not dead, or you are controlling a non-living entity.")
                return true
            }

            return false
        } else if (message.startsWith("chat/")) {
            var chatMessage = message.substring(5, message.length)

            // Messages starting with / are commands
            if (chatMessage.startsWith("/")) {
                chatMessage = chatMessage.substring(1, chatMessage.length)

                var cmdName = chatMessage.toLowerCase()
                var args = arrayOf<String>()
                if (chatMessage.contains(" ")) {
                    cmdName = chatMessage.substring(0, chatMessage.indexOf(" "))
                    args = chatMessage.substring(chatMessage.indexOf(" ") + 1, chatMessage.length).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                }

                clientsManager.getServer().console.dispatchCommand(player, cmdName, args)
                return true
                // The rest is just normal chat
            } else if (chatMessage.length > 0) {
                val event = PlayerChatEvent(player, chatMessage)
                clientsManager.getServer().pluginManager.fireEvent(event)

                if (!event.isCancelled)
                    context.broadcastMessage(event.formattedMessage)

                return true
            } else {
                return true // Ignore empty messages
            }
        }
        return false
    }

    override fun sendTextMessage(string: String) {
        super.sendTextMessage(string)
        if (player == null)
        // Flush the pipe automatically when the player isn't yet logged in
            this.flush()
    }

    internal fun associatePlayer(player: ServerPlayer) {
        this.player = player;
        this.encoderDecoder = this.encoderDecoder.toPlayer(player)
        this.clientsManager.registerPlayer(this)
    }

    /** Disconnects the remote party but tells them why  */
    fun disconnect(disconnectionReason: String) {
        this.sendTextMessage("disconnect/$disconnectionReason")
        this.flush()
        this.close(disconnectionReason)
    }

    override fun close(reason: String) {
        logger.info("Disconnecting $this :$reason")
        clientsManager.removeClient(this)

        if (player != null) {
            val playerDisconnectionEvent = PlayerLogoutEvent(player!!)
            clientsManager.getServer().pluginManager.fireEvent(playerDisconnectionEvent)
            clientsManager.getServer().broadcastMessage(playerDisconnectionEvent.logoutMessage!!)

            player!!.destroy()
        }
    }

    companion object {

        /**
         * Keeps track of how many client connections were opened to assign them an
         * unique id (debug purposes)
         */
        private val usersCount = AtomicInteger()
    }
}
