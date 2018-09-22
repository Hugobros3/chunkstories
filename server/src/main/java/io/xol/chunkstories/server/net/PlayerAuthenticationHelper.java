//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net;

import io.xol.chunkstories.api.events.player.PlayerLoginEvent;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.net.http.SimplePostRequest;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class to offload the login handling logic from ClientConnection */
class PlayerAuthenticationHelper {
	private final ClientConnection connection;

	private String name, token, version;
	private boolean loggedIn = false;

	private static final Logger logger = LoggerFactory.getLogger("server.authentication");

	PlayerAuthenticationHelper(ClientConnection connection) {
		this.connection = connection;

		logger.debug("User on connection "+connection+" attempting to authenticate...");
	}

	boolean handleLogin(String loginRequest) {
		if (loggedIn)
			return false;

		if (loginRequest.startsWith("username:")) {
			this.name = loginRequest.replace("username:", "");
			return true;
		}
		if (loginRequest.startsWith("logintoken:")) {
			token = loginRequest.replace("logintoken:", "");
			return true;
		}
		if (loginRequest.startsWith("version:")) {
			version = loginRequest.replace("version:", "");
			return true;
		}

		if (loginRequest.startsWith("confirm")) {
			if (name.equals("undefined"))
				return true;
			if (connection.clientsManager.getServer().getUserPrivileges().isUserBanned(name)) {
				connection.disconnect("Banned username - " + name);
				return true;
			}
			if (token.length() != 20) {
				connection.disconnect("No valid token supplied");
				return true;
			}
			if (connection.clientsManager.getServer().getServerConfig().getBooleanValue("server.security.checkClientVersion")) {
				if (Integer.parseInt(version) != VersionInfo.networkProtocolVersion)
					connection.disconnect("Wrong protocol version ! " + version + " != "+ VersionInfo.networkProtocolVersion + " \n Update your game !");
			}
			if (connection.clientsManager.getServer().getServerConfig().getBooleanValue("server.security.checkClientAuthentification")) {
				connection.logger.warn("Offline-mode is on, letting " + this.name + " connecting without verification");
				afterLoginValidation();
				return true;
			} else {
				// Send an async https request & notify of the results later
				new SimplePostRequest("https://chunkstories.xyz/api/serverTokenChecker.php",
						"username=" + this.name + "&token=" + token, (result) -> {
							if (result != null && result.equals("ok"))
								afterLoginValidation();
							else
								connection.disconnect("Invalid session id !");
						});
				return true;
			}
		}

		return false;
	}

	/**
	 * Called after the login token has been validated, or in the case of an
	 * offline-mode server, after the client requested to login.
	 */
	private void afterLoginValidation() {
		// Disallow users from logging in from two places
		Player yourEvilDouble = connection.clientsManager.getPlayerByName(name);
		if (yourEvilDouble != null) {
			connection.disconnect("You are already logged in. (" + yourEvilDouble + "). ");
			return;
		}

		// Creates a player based on the thrusted login information
		ServerPlayer player = new ServerPlayer(connection, name);

		// Fire the login event
		PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(player);
		connection.clientsManager.getServer().getPluginManager().fireEvent(playerConnectionEvent);
		if (playerConnectionEvent.isCancelled()) {
			connection.disconnect(playerConnectionEvent.getRefusedConnectionMessage());
			return;
		}

		// Announce player login
		connection.clientsManager.getServer().broadcastMessage(playerConnectionEvent.getConnectionMessage());

		// Aknowledge the login
		loggedIn = true;
		connection.sendTextMessage("login/ok");
		connection.flush();
		connection.setPlayer(player);
	}
}
