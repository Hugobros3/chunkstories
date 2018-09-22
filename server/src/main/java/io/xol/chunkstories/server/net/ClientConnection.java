//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.events.player.PlayerChatEvent;
import io.xol.chunkstories.api.events.player.PlayerLogoutEvent;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.content.translator.AbstractContentTranslator;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.packets.PacketContentTranslator;
import io.xol.chunkstories.net.packets.PacketSendFile;
import io.xol.chunkstories.net.packets.PacketSendWorldInfo;
import io.xol.chunkstories.server.net.ServerPacketsProcessorImplementation.ClientPacketsContext;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;

public abstract class ClientConnection extends Connection implements Interlocutor {

	protected final Server server;
	protected final ClientsManager clientsManager;

	protected final Logger logger;

	private PlayerAuthenticationHelper loginHelper;

	protected ClientPacketsContext packetsProcessor;
	protected ServerPlayer player = null;

	/**
	 * Keeps track of how many client connections were opened to assign them an
	 * unique id (debug purposes)
	 */
	private static final AtomicInteger usersCount = new AtomicInteger();

	public ClientConnection(Server server, ClientsManager clientsManager, String remoteAddress, int port) {
		super(remoteAddress, port);
		this.server = server;
		this.clientsManager = clientsManager;

		// This way we can tell the logs from one use to the next
		this.logger = LoggerFactory.getLogger("server.net.users." + usersCount.getAndIncrement());

		this.loginHelper = new PlayerAuthenticationHelper(this);
	}

	public Server getContext() {
		return server;
	}

	@Override
	public ClientPacketsContext getEncoderDecoder() {
		return packetsProcessor;
	}

	@Override
	public boolean connect() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean handleSystemRequest(String message) {
		if (message.startsWith("worldInfo")) {
			this.clientsManager.sendServerInfo(this);
			return true;

		} else if (message.startsWith("login/")) {
			return loginHelper.handleLogin(message.substring(6, message.length()));

		} else if (message.equals("mods")) {
			sendTextMessage("worldInfo/mods:" + clientsManager.getServer().getModsProvider().getModsString());
			this.flush();
			return true;

		} else if (message.equals("icon-file")) {
			PacketSendFile iconPacket = new PacketSendFile();
			iconPacket.file = new File("server-icon.png");
			iconPacket.fileTag = "server-icon";
			this.pushPacket(iconPacket);
			this.flush();
			return true;
		}

		// Any other commands need an authentificated player !
		if (player == null)
			return false;

		// Login-mandatory requests ( you need to be authentificated to use them )
		if (message.equals("co/off")) {
			this.disconnect("Client-terminated connection");

		} else if (message.startsWith("send-mod/")) {
			String modDescriptor = message.substring(9);

			String md5 = modDescriptor.substring(4);
			logger.info(this + " asked to be sent mod " + md5);

			// Give him what he asked for.
			File found = clientsManager.getServer().getModsProvider().obtainModRedistribuable(md5);
			if (found == null) {
				logger.info("No such mod found.");
			} else {
				logger.info("Pushing mod md5 " + md5 + "to user.");
				PacketSendFile modUploadPacket = new PacketSendFile();
				modUploadPacket.file = found;
				modUploadPacket.fileTag = modDescriptor;
				this.pushPacket(modUploadPacket);
			}

		} else if (message.startsWith("world/")) {
			// TODO this bit will obviously need to be rewritten when I get arround to doing
			// multiworld support
			WorldServer world = clientsManager.getServer().getWorld();
			message = message.substring(6, message.length());

			if (message.equals("enter")) {
				player.setWorld(world);
				// Sends the construction worldInfo for the world, and then the player entity
				PacketSendWorldInfo packet = new PacketSendWorldInfo(world.getWorldInfo());
				pushPacket(packet);

				// TODO only spawn the player when he asks to
				world.spawnPlayer(player);
				return true;

			} else if (message.equals("translator")) {
				PacketContentTranslator packet = new PacketContentTranslator(
						(AbstractContentTranslator) world.getContentTranslator());
				player.pushPacket(packet);
				return true;

			} else if (message.equals("respawn")) {
				// Only allow to respawn if the current entity is null or dead
				if (player.getControlledEntity() == null
						|| player.getControlledEntity().traits.tryWithBoolean(TraitHealth.class, eh -> eh.isDead())) {
					world.spawnPlayer(player);
					player.sendMessage("Respawning ...");
				} else
					player.sendMessage("You're not dead, or you are controlling a non-living entity.");
				return true;
			}

			return false;
		} else if (message.startsWith("chat/")) {
			String chatMessage = message.substring(5, message.length());

			// Messages starting with / are commands
			if (chatMessage.startsWith("/")) {
				chatMessage = chatMessage.substring(1, chatMessage.length());

				String cmdName = chatMessage.toLowerCase();
				String[] args = {};
				if (chatMessage.contains(" ")) {
					cmdName = chatMessage.substring(0, chatMessage.indexOf(" "));
					args = chatMessage.substring(chatMessage.indexOf(" ") + 1, chatMessage.length()).split(" ");
				}

				clientsManager.getServer().getConsole().dispatchCommand(player, cmdName, args);
				return true;
				// The rest is just normal chat
			} else if (chatMessage.length() > 0) {
				PlayerChatEvent event = new PlayerChatEvent(player, chatMessage);
				clientsManager.getServer().getPluginManager().fireEvent(event);

				if (!event.isCancelled())
					server.broadcastMessage(event.getFormattedMessage());

				return true;
			} else {
				return true; // Ignore empty messages
			}
		}
		return false;
	}

	@Override
	public void sendTextMessage(String string) {
		super.sendTextMessage(string);
		if (player == null) // Flush the pipe automatically when the player isn't yet logged in
			this.flush();
	}

	public void setPlayer(ServerPlayer player) {
		if (this.player == null) {
			this.player = player;
			this.packetsProcessor = this.packetsProcessor.toPlayer(player);

			clientsManager.registerPlayer(this);
		}
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	@Override
	public void disconnect() {
		close();
	}

	@Override
	public void disconnect(String disconnectionReason) {
		// TODO send reason
		logger.info("Disconnecting " + this + " reason:" + disconnectionReason);
		close();
	}

	@Override
	public boolean close() {
		clientsManager.removeClient(this);

		if (player != null) {
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(player);
			clientsManager.getServer().getPluginManager().fireEvent(playerDisconnectionEvent);
			clientsManager.getServer().broadcastMessage(playerDisconnectionEvent.getLogoutMessage());

			player.destroy();
		}
		return true;
	}
}
