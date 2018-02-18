package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.events.player.PlayerLoginEvent;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.engine.net.SimplePostRequest;

/** Helper class to offload the login handling logic from ClientConnection */
public class PlayerLoginHelper {
	final ClientConnection connection;

	String name, token, version;
	
	boolean logged_in = false;

	public PlayerLoginHelper(ClientConnection connection) {
		this.connection = connection;
	}

	public boolean handleLogin(String loginRequest) {
		if(logged_in)
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
			if (connection.clientsManager.getServer().getServerConfig().getString("check-version", "true")
					.equals("true")) {

				if (Integer.parseInt(version) != VersionInfo.networkProtocolVersion)
					connection.disconnect("Wrong protocol version ! " + version + " != "
							+ VersionInfo.networkProtocolVersion + " \n Update your game !");
			}
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
			if (connection.clientsManager.getServer().getServerConfig().getInteger("offline-mode", 0) == 1) {
				connection.logger.warn("Offline-mode is on, letting " + this.name + " connecting without verification");
				afterLoginValidation();
				return true;
			} else {
				//Send an async https request & notify of the results later
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
		Player contender = connection.clientsManager.getPlayerByName(name);
		if (contender != null) {
			connection.disconnect("You are already logged in. (" + contender + "). ");
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
		logged_in = true;
		connection.sendTextMessage("login/ok");
		connection.flush();
		connection.setPlayer(player);
	}
}
