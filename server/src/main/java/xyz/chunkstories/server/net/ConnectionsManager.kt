//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net

import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.net.http.SimpleWebRequest
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.DedicatedServerOptions
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.util.VersionInfo
import java.util.concurrent.ConcurrentHashMap

abstract class ConnectionsManager(val server: DedicatedServer) {
    var hostname: String
        protected set
    var ip: String
        protected set
    val maxClients: Int
        get() = server.config.getIntValue(DedicatedServerOptions.maxUsers)

    val connections: MutableSet<ClientConnection> = ConcurrentHashMap.newKeySet()
    internal val authenticatedPlayers: MutableMap<PlayerID, ServerPlayer> = ConcurrentHashMap()

    val allConnectedClients: MutableIterator<ClientConnection>
        get() = connections.iterator()

    val playersNumber: Int
        get() = authenticatedPlayers.size

    init {
        // TODO add config option for disabling this stuff
        ip = SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?ip=1").waitForResult()
        hostname = SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?host=1").waitForResult()
    }

    abstract fun open()

    fun flushAll() {
        for (client in connections)
            client.flush()
    }

    open fun terminate() {
       for(client in connections) {
           client.disconnect("Server is closing.")
       }
    }

    fun getPlayerByName(playerName: String) = authenticatedPlayers.values.find { it.name == playerName }

    internal fun sendServerInfo(clientConnection: ClientConnection) {
        clientConnection.sendTextMessage("info/name:" + server.config.getValue(DedicatedServerOptions.serverName))
        clientConnection.sendTextMessage("info/motd:" + server.config.getValue(DedicatedServerOptions.serverDescription))
        clientConnection.sendTextMessage("info/connected:$playersNumber:$maxClients")
        clientConnection.sendTextMessage("info/version:" + VersionInfo.versionJson.version)
        clientConnection.sendTextMessage("info/mods:" + server.modsProvider.modsString)
        clientConnection.sendTextMessage("info/done")

        // We flush because since the potential player isn't registered, the automatic
        // flush at world ticking doesn't apply to them
        clientConnection.flush()
    }

    // Called once the player successfully authenticated
    internal fun registerPlayer(clientConnection: ClientConnection) {
        val player = clientConnection.player!!
        this.authenticatedPlayers[player.id] = player
    }

    // Called when close() is called on the connection
    internal fun removeClient(clientConnection: ClientConnection) {
        this.connections.remove(clientConnection)

        val player = clientConnection.player
        if (player != null) {
            this.authenticatedPlayers.remove(player.id)
        }
    }

}