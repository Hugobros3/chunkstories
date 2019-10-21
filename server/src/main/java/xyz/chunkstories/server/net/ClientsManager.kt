//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net

import xyz.chunkstories.api.player.Player
import xyz.chunkstories.net.http.SimpleWebRequest
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.DedicatedServerOptions
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.util.VersionInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class ClientsManager(val server: DedicatedServer) {
    val packetsProcessor: ServerPacketsProcessorImplementation

    var hostname: String
        protected set
    var ip: String
        protected set

    var maxClients: Int = 0
        protected set

    protected var clients: MutableSet<ClientConnection> = ConcurrentHashMap.newKeySet()

    protected var playersMap: MutableMap<String, ServerPlayer> = ConcurrentHashMap()
    protected var playersByUUID: MutableMap<Long, ServerPlayer> = ConcurrentHashMap()

    val allConnectedClients: MutableIterator<ClientConnection>
        get() = clients.iterator()

    val playersNumber: Int
        get() = playersMap.size

    init {
        this.packetsProcessor = ServerPacketsProcessorImplementation(server)

        ip = SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?ip=1").waitForResult()
        hostname = SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?host=1").waitForResult()

        this.maxClients = server.serverConfig.getIntValue(DedicatedServerOptions.maxUsers)
    }

    abstract fun open(): Boolean

    fun flushAll() {
        for (client in clients)
            client.flush()
    }

    open fun close(): Boolean {
        val clientsIterator = allConnectedClients
        while (clientsIterator.hasNext()) {
            val client = clientsIterator.next()
            // Remove the client first to avoid concurrent mod exception
            clientsIterator.remove()

            client.disconnect("Server is closing.")
        }

        return true
    }

    fun getPlayerByName(playerName: String): Player? {
        return playersMap[playerName]
    }

    val players: Set<Player>
        get() {
            return LinkedHashSet(playersMap.values)
        }

    fun sendServerInfo(clientConnection: ClientConnection) {
        clientConnection.sendTextMessage("info/name:" + server.serverConfig.getValue(DedicatedServerOptions.serverName))
        clientConnection.sendTextMessage("info/motd:" + server.serverConfig.getValue(DedicatedServerOptions.serverDescription))
        clientConnection.sendTextMessage("info/connected:$playersNumber:$maxClients")
        clientConnection.sendTextMessage("info/version:" + VersionInfo.versionJson.version)
        clientConnection.sendTextMessage("info/mods:" + server.modsProvider.modsString)
        clientConnection.sendTextMessage("info/done")

        // We flush because since the potential player isn't registered, the automatic
        // flush at world ticking doesn't apply to them
        clientConnection.flush()
    }

    /**
     * Used by ClientConnection after a successfull login procedure
     */
    internal fun registerPlayer(clientConnection: ClientConnection) {
        val player = clientConnection.player
        this.playersMap[player!!.name] = player
        this.playersByUUID[player.uuid] = player
    }

    /**
     * Used by ClientConnection during the close() method
     */
    internal fun removeClient(clientConnection: ClientConnection) {
        this.clients.remove(clientConnection)

        val player = clientConnection.player
        if (player != null) {
            this.playersMap.remove(player.name)
            this.playersByUUID.remove(player.uuid)
        }
    }

}