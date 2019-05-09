//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net;

import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.net.http.SimpleWebRequest;
import xyz.chunkstories.server.DedicatedServer;
import xyz.chunkstories.server.DedicatedServerOptions;
import xyz.chunkstories.server.player.ServerPlayer;
import xyz.chunkstories.util.VersionInfo;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ClientsManager {

	protected final DedicatedServer server;
	protected final ServerPacketsProcessorImplementation packetsProcessor;

	protected String hostname;
	protected String externalIP;

	protected int maxClients;

	protected Set<ClientConnection> clients = ConcurrentHashMap.newKeySet();

	protected Map<String, ServerPlayer> players = new ConcurrentHashMap<>();
	protected Map<Long, ServerPlayer> playersByUUID = new ConcurrentHashMap<>();

	public ClientsManager(DedicatedServer server) {
		this.server = server;
		this.packetsProcessor = new ServerPacketsProcessorImplementation(server);

		externalIP = new SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?ip=1").waitForResult();
		hostname = new SimpleWebRequest("https://chunkstories.xyz/api/sayMyName.php?host=1").waitForResult();

		this.maxClients = server.getServerConfig().getIntValue(DedicatedServerOptions.INSTANCE.getMaxUsers());
	}

	public DedicatedServer getServer() {
		return server;
	}

	public abstract boolean open();

	public void flushAll() {
		for (ClientConnection client : clients)
			client.flush();
	}

	public boolean close() {
		Iterator<ClientConnection> clientsIterator = getAllConnectedClients();
		while (clientsIterator.hasNext()) {
			ClientConnection client = clientsIterator.next();
			// Remove the client first to avoid concurrent mod exception
			clientsIterator.remove();

			client.disconnect("Server is closing.");
		}

		return true;
	}

	public int getMaxClients() {
		return maxClients;
	}

	public Iterator<ClientConnection> getAllConnectedClients() {
		return clients.iterator();
	}

	public String getIP() {
		return externalIP;
	}

	public String getHostname() {
		return hostname;
	}

	public ServerPacketsProcessorImplementation getPacketsProcessor() {
		return this.packetsProcessor;
	}

	public int getPlayersNumber() {
		return players.size();
	}

	public Player getPlayerByName(String playerName) {
		return players.get(playerName);
	}

	public Set<Player> getPlayers() {
		return new LinkedHashSet(players.values());
		/*return new IterableIterator<Player>() {
			Iterator<ServerPlayer> authClients = players.values().iterator();

			@Override
			public boolean hasNext() {
				return authClients.hasNext();
			}

			@Override
			public ServerPlayer next() {
				return authClients.next();
			}
		};*/
	}

	public void sendServerInfo(ClientConnection clientConnection) {
		clientConnection.sendTextMessage("info/name:" + getServer().getServerConfig().getValue(DedicatedServerOptions.INSTANCE.getServerName()));
		clientConnection.sendTextMessage("info/motd:" + getServer().getServerConfig().getValue(DedicatedServerOptions.INSTANCE.getServerDescription()));
		clientConnection.sendTextMessage("info/connected:" + getPlayersNumber() + ":" + getMaxClients());
		clientConnection.sendTextMessage("info/version:" + VersionInfo.version);
		clientConnection.sendTextMessage("info/mods:" + getServer().getModsProvider().getModsString());
		clientConnection.sendTextMessage("info/done");

		// We flush because since the potential player isn't registered, the automatic
		// flush at world ticking doesn't apply to them
		clientConnection.flush();
	}

	/**
	 * Used by ClientConnection after a successfull login procedure
	 */
	void registerPlayer(ClientConnection clientConnection) {
		ServerPlayer player = clientConnection.getPlayer();
		this.players.put(player.getName(), player);
		this.playersByUUID.put(player.getUuid(), player);
	}

	/**
	 * Used by ClientConnection during the close() method
	 */
	void removeClient(ClientConnection clientConnection) {
		this.clients.remove(clientConnection);

		ServerPlayer player = clientConnection.getPlayer();
		if (player != null) {
			this.players.remove(player.getName());
			this.playersByUUID.remove(player.getUuid());
		}
	}

}