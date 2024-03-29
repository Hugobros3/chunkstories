//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.events.player.PlayerLoginEvent
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.server.DedicatedServerOptions
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.util.VersionInfo
import java.util.*

/**
 * Helper class to offload the login handling logic from ClientConnection
 */
//TODO use a f'in pojo & json dude
internal class PlayerAuthenticationHelper(private val connection: ClientConnection) {

    private var name: String? = null
    private var token: String? = null
    private var version: String? = null
    private var loggedIn = false

    init {

        logger.debug("User on connection $connection attempting to authenticate...")
    }

    fun handleLogin(loginRequest: String): Boolean {
        if (loggedIn)
            return false

        if (loginRequest.startsWith("username:")) {
            this.name = loginRequest.replace("username:", "")
            return true
        }
        if (loginRequest.startsWith("logintoken:")) {
            token = loginRequest.replace("logintoken:", "")
            return true
        }
        if (loginRequest.startsWith("version:")) {
            version = loginRequest.replace("version:", "")
            return true
        }

        if (loginRequest.startsWith("confirm")) {
            if (name == "undefined")
                return true
            if (connection.connectionsManager.server.userPrivileges.bannedUsers.contains(name)) {
                connection.disconnect("Banned username - " + name!!)
                return true
            }
            if (token!!.length != 20) {
                connection.disconnect("No valid token supplied")
                return true
            }
            if (connection.connectionsManager.server.config.getBooleanValue(DedicatedServerOptions.checkClientVersion)) {
                if (Integer.parseInt(version!!) != VersionInfo.networkProtocolVersion)
                    connection.disconnect("Wrong protocol version ! " + version + " != " + VersionInfo.networkProtocolVersion + " \n Update your game !")
            }
            if (true || !connection.connectionsManager.server.config.getBooleanValue(DedicatedServerOptions.checkClientAuthentication)) {
                connection.logger.warn("Offline-mode is on, letting " + this.name + " connecting without verification")
                afterLoginValidation()
                return true
            } else {
                /*// Send an async https request & notify of the results later
                SimplePostRequest("https://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token) { result ->
                    if (result != null && result == "ok")
                        afterLoginValidation()
                    else
                        connection.disconnect("Invalid session id !")
                }
                return true*/
                TODO("Rewrite this")
            }
        }

        return false
    }

    /**
     * Called after the login token has been validated, or in the case of an
     * offline-mode server, after the client requested to login.
     */
    private fun afterLoginValidation() {
        // Disallow users from logging in from two places
        val yourEvilDouble = connection.connectionsManager.getPlayerByName(name!!)
        if (yourEvilDouble != null) {
            connection.disconnect("You are already logged in. ($yourEvilDouble). ")
            return
        }

        // Creates a player based on the thrusted login information
        // TODO fix UUID nonsense
        val player = ServerPlayer(connection, PlayerID(UUID.fromString(name)), name!!)

        // Fire the login event
        val playerConnectionEvent = PlayerLoginEvent(player)
        connection.server.pluginManager.fireEvent(playerConnectionEvent)
        if (playerConnectionEvent.isCancelled) {
            connection.disconnect(playerConnectionEvent.refusedConnectionMessage)
            return
        }

        // Announce player login
        connection.server.broadcastMessage(playerConnectionEvent.connectionMessage!!)

        // Aknowledge the login
        loggedIn = true
        connection.sendTextMessage("login/ok")
        connection.flush()
        connection.associatePlayer(player)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("server.authentication")
    }
}
