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
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.net.Connection
import xyz.chunkstories.net.packets.PacketContentTranslator
import xyz.chunkstories.net.packets.PacketSendFile
import xyz.chunkstories.net.packets.PacketSendWorldInfo
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.world.spawnPlayer

abstract class ClientConnection(val server: DedicatedServer, internal val connectionsManager: ConnectionsManager, remoteAddress: String, port: Int) : Connection(remoteAddress, port) {
    internal val logger: Logger

    private val loginHelper: PlayerAuthenticationHelper

    var player: ServerPlayer? = null

    init {
        // This way we can tell the logs from one use to the next
        this.logger = LoggerFactory.getLogger("server.net.users." + usersCount.getAndIncrement())
        this.loginHelper = PlayerAuthenticationHelper(this)
    }

    override fun connect(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun handleSystemRequest(message: String): Boolean {
        when {
            message.startsWith("info") -> {
                this.connectionsManager.sendServerInfo(this)
                return true
            }
            message.startsWith("login/") -> {
                return loginHelper.handleLogin(message.substring(6, message.length))
            }
            message == "mods" -> {
                sendTextMessage("info/mods:" + connectionsManager.server.modsProvider.modsString)
                this.flush()
                return true
            }
            message == "icon-file" -> {
                val iconPacket = PacketSendFile()
                iconPacket.file = File("server-icon.png")
                iconPacket.fileTag = "server-icon"
                this.pushPacket(iconPacket)
                this.flush()
                return true
            }
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
            val found = connectionsManager.server.modsProvider.obtainModRedistribuable(md5)
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
            // TODO this cannot support multi-world servers
            val world = connectionsManager.server.world
            when (message.substring(6, message.length)) {
                "enter" -> {
                    player.whenEnteringWorld(world)
                    // Sends the construction info for the world, and then the player entity
                    val packet = PacketSendWorldInfo(world.properties)
                    pushPacket(packet)

                    // TODO only spawn the player when he asks to
                    world.spawnPlayer(player)
                    return true
                }
                "translator" -> {
                    val packet = PacketContentTranslator(world.contentTranslator)
                    player.pushPacket(packet)
                    return true
                }
                "respawn" -> {
                    // Only allow to respawn if the current entity is null or dead
                    val playerEntity = player.entityIfIngame
                    if (playerEntity == null || playerEntity.traits[TraitHealth::class]?.isDead == true) {
                        world.spawnPlayer(player)
                        player.sendMessage("Respawning ...")
                    } else
                        player.sendMessage("You're not dead, or you are controlling a non-living entity.")
                    return true
                }
                else -> return false
            }

        } else if (message.startsWith("chat/")) {
            val chatMessage = message.substring(5, message.length)
            when {
                chatMessage.startsWith("/") -> {
                    val commandString = chatMessage.substring(1, chatMessage.length)

                    var cmdName = commandString.toLowerCase()
                    var args = arrayOf<String>()
                    if (commandString.contains(" ")) {
                        cmdName = commandString.substring(0, commandString.indexOf(" "))
                        args = commandString.substring(commandString.indexOf(" ") + 1, commandString.length).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    }

                    connectionsManager.server.dispatchCommand(player, cmdName, args)
                }
                chatMessage.isNotEmpty() -> {
                    val event = PlayerChatEvent(player, chatMessage)
                    connectionsManager.server.pluginManager.fireEvent(event)

                    if (!event.isCancelled)
                        server.broadcastMessage(event.formattedMessage)
                }
            }
            return true
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
        this.player = player
        this.connectionsManager.registerPlayer(this)
    }

    /** Disconnects the remote party but tells them why */
    fun disconnect(disconnectionReason: String) {
        this.sendTextMessage("disconnect/$disconnectionReason")
        this.flush()
        this.close(disconnectionReason)
    }

    override fun close(reason: String) {
        logger.info("Disconnecting $this :$reason")
        connectionsManager.removeClient(this)

        if (player != null) {
            val playerDisconnectionEvent = PlayerLogoutEvent(player!!)
            connectionsManager.server.pluginManager.fireEvent(playerDisconnectionEvent)
            connectionsManager.server.broadcastMessage(playerDisconnectionEvent.logoutMessage!!)

            player!!.destroy()
        }
    }

    companion object {
        private val usersCount = AtomicInteger()
    }
}
