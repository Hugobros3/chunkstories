package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.events.player.PlayerChatEvent;
import io.xol.chunkstories.api.events.player.PlayerLoginEvent;
import io.xol.chunkstories.api.events.player.PlayerLogoutEvent;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.net.SendQueue;
import io.xol.chunkstories.net.packets.PacketSendFile;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.UsersPrivileges;
import io.xol.chunkstories.server.net.ServerPacketsProcessorImplementation.UserPacketsProcessor;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class UserConnection extends Thread implements HttpRequester, PacketDestinator, PacketSender
{
	private final ServerConnectionsManager connectionsManager;
	private final ChunkStoriesLogger logger;

	int socketPort = 0;

	//Streams.
	Socket socket;
	UserPacketsProcessor packetsProcessor;
	DataInputStream inputDataStream = null;
	SendQueue sendQueue;

	boolean validToken = false;
	String token = "undefined";

	//Did the connection died at some point ?
	private AtomicBoolean calledCloseSockedOnce = new AtomicBoolean(false);
	public String name = "undefined";
	//Version of the client he uses
	public String version = "undefined";

	//Assertion : if the player is authentificated it has a profile
	private ServerPlayer player;

	private PacketSender sender = this;

	UserConnection(ServerConnectionsManager connectionsManager, Socket socket) throws FailedToConnectionException
	{
		this.connectionsManager = connectionsManager;
		this.logger = connectionsManager.getServer().logger();

		this.socket = socket;
		this.socketPort = socket.getPort();

		this.packetsProcessor = connectionsManager.getPacketsProcessor().forConnection(this);//new PacketsProcessor(this, connectionsManager.getServer().getContent().packets());
		this.setName(this.toString());

		try
		{
			this.openSocket();
			this.start();
		}
		catch (IOException e)
		{
			System.out.println("Failed to open connection to " + this.getIp() + " - " + e.getMessage());
			throw new FailedToConnectionException();
		}
	}

	public void openSocket() throws IOException
	{
		//We get exceptions early if this fails
		InputStream socketInputStream = socket.getInputStream();
		OutputStream socketOutputStream = socket.getOutputStream();

		inputDataStream = new DataInputStream(new BufferedInputStream(socketInputStream));

		//The sendQueue is initialized with 'this' as the destinator, 'this', the ServerClient class does not implement the Subscriber interface
		//but provides the PacketDestinator interface so outgoing packets can know what kind of client they are talking to ( in this very case, an unauthentificated client )
		//See : postTokenCheck() for more information
		sendQueue = new SendQueue(this, new DataOutputStream(new BufferedOutputStream(socketOutputStream)), packetsProcessor);
		sendQueue.start();
	}

	class FailedToConnectionException extends Exception
	{

		private static final long serialVersionUID = 3423402904369758447L;

	}

	// Here's the usefull things !

	public void handle(String textRequest)
	{
		// These requests don't require a login
		if (textRequest.startsWith("info"))
			this.sendInformationAboutServer();
		else if (textRequest.startsWith("login/"))
			this.handleLogin(textRequest.substring(6, textRequest.length()));
		else if (textRequest.equals("mods"))
			this.sendInternalTextMessage("info/mods:" + connectionsManager.getServer().getModsProvider().getModsString());
		else if (textRequest.equals("icon-file"))
		{
			PacketSendFile iconPacket = new PacketSendFile();
			iconPacket.file = new File("server-icon.png");
			iconPacket.fileTag = "server-icon";
			this.pushPacket(iconPacket);
			this.flush();
		}
		// Checks for auth - ignores unauthrorized requests
		if (!this.isAuthentificated())
			return;

		// Login-mandatory requests ( you need to be authentificated to use them )
		if (textRequest.equals("co/off"))
		{
			this.disconnect("Client-terminated connection");
		}

		else if (textRequest.startsWith("send-mod/"))
		{
			String modDescriptor = textRequest.substring(9);

			String md5 = modDescriptor.substring(4);
			System.out.println(this + " asked for " + md5);

			//Give him what he asked for.
			File found = connectionsManager.getServer().getModsProvider().obtainModRedistribuable(md5);
			if (found == null)
			{
				System.out.println("Though luck !");
			}
			else
			{
				System.out.println("Pushing mod md5 " + md5 + "to user.");
				PacketSendFile modUploadPacket = new PacketSendFile();
				modUploadPacket.file = found;
				modUploadPacket.fileTag = modDescriptor;
				this.pushPacket(modUploadPacket);
			}
		}
		else if (textRequest.startsWith("world/"))
		{
			connectionsManager.getServer().getWorld().handleWorldMessage(this, textRequest.substring(6, textRequest.length()));
		}
		else if (textRequest.startsWith("chat/"))
		{
			String chatMessage = textRequest.substring(5, textRequest.length());

			// Console commands parsing login
			if (chatMessage.startsWith("/"))
			{
				chatMessage = chatMessage.substring(1, chatMessage.length());

				String cmdName = chatMessage.toLowerCase();
				String[] args = {};
				if (chatMessage.contains(" "))
				{
					cmdName = chatMessage.substring(0, chatMessage.indexOf(" "));
					args = chatMessage.substring(chatMessage.indexOf(" ") + 1, chatMessage.length()).split(" ");
				}

				// Sa degage
				connectionsManager.getServer().getConsole().dispatchCommand(this.getProfile(), cmdName, args);
			}
			else if (chatMessage.length() > 0)
			{
				PlayerChatEvent event = new PlayerChatEvent(this.getProfile(), chatMessage);
				connectionsManager.getServer().getPluginManager().fireEvent(event);
				
				if(!event.isCancelled())
					connectionsManager.broadcastChatMessage(event.getFormattedMessage());
			}
		}
	}

	public void handleLogin(String loginRequest)
	{
		if (loginRequest.startsWith("username:"))
		{
			this.name = loginRequest.replace("username:", "");
		}
		if (loginRequest.startsWith("logintoken:"))
		{
			token = loginRequest.replace("logintoken:", "");
		}
		if (loginRequest.startsWith("version:"))
		{
			version = loginRequest.replace("version:", "");
			if (connectionsManager.getServer().getServerConfig().getString("check-version", "true").equals("true"))
			{
				if (Integer.parseInt(version) != VersionInfo.networkProtocolVersion)
					disconnect("Wrong protocol version ! " + version + " != " + VersionInfo.networkProtocolVersion + " \n Update your game !");
			}
		}
		if (loginRequest.startsWith("confirm"))
		{
			if (name.equals("undefined"))
				return;
			if (UsersPrivileges.isUserBanned(name))
			{
				disconnect("Banned username - " + name);
				return;
			}
			if (token.length() != 20)
			{
				disconnect("No valid token supplied");
				return;
			}
			if (connectionsManager.getServer().getServerConfig().getInteger("offline-mode", 0) == 1)
			{
				// Offline-mode !
				System.out.println("Warning : Offline-mode is on, letting " + this.name + " connecting without verification");
				afterLoginValidation();
			}
			else
				new HttpRequestThread(this, "checktoken", "http://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token);
		}
	}

	/**
	 * Sends general information about the server
	 */
	void sendInformationAboutServer()
	{
		this.sendInternalTextMessage("info/name:" + getServer().getServerConfig().getString("server-name", "unnamedserver@" + connectionsManager.getHostname()));
		this.sendInternalTextMessage("info/motd:" + getServer().getServerConfig().getString("server-desc", "Default description."));
		this.sendInternalTextMessage("info/connected:" + getServer().getHandler().getNumberOfAuthentificatedClients() + ":" + connectionsManager.getMaxClients());
		this.sendInternalTextMessage("info/version:" + VersionInfo.version);
		this.sendInternalTextMessage("info/mods:" + getServer().getModsProvider().getModsString());
		this.sendInternalTextMessage("info/done");
		
		//We flush because since the potential player isn't registered, the automatic flush at world ticking doesn't apply to them
		this.flush();
	}

	@Override
	public void run()
	{
		//Just reads packets and processes them
		while (!this.calledCloseSockedOnce.get())
		{
			try
			{
				Packet packet = packetsProcessor.getPacket(inputDataStream);
				packet.process(sender, inputDataStream, packetsProcessor);
			}
			catch (IllegalPacketException | UnknowPacketException e)
			{
				logger.info("Disconnected " + this + " for causing an " + e.getClass().getSimpleName());
				e.printStackTrace();
				disconnect("Exception : " + e.getMessage());
				return;
			}
			catch (IOException e)
			{
				if (this.isAuthentificated())
					logger.info("Connection lost to " + this.getProfile().getDisplayName() + " (" + this.getName() + ").");

				disconnect("Broken socket");
				return;
			}
			catch (Exception e)
			{
				logger.info("Disconnected " + this + " for causing an " + e.getClass().getSimpleName());
				e.printStackTrace();
				disconnect("Exception : " + e.getMessage());
				return;
			}
		}
		disconnect("Socket terminated");
	}

	public String getIp()
	{
		InetAddress inet = socket.getInetAddress();

		if (inet == null)
			return "[IP:Unconnected socket]";
		return inet.getHostAddress();
	}

	public String getHostname()
	{
		InetAddress inet = socket.getInetAddress();

		if (inet == null)
			return "[HN:Unconnected socket]";
		return inet.getHostName();
	}

	void closeSocket()
	{
		if (calledCloseSockedOnce.compareAndSet(false, true))
		{
			if (getProfile() != null)
			{
				PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(getProfile());
				connectionsManager.getServer().getPluginManager().fireEvent(playerDisconnectionEvent);

				connectionsManager.getServer().getHandler().broadcastChatMessage(playerDisconnectionEvent.getLogoutMessage());

				//connectionsManager.getServer().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ") left.");
				assert getProfile() != null;
				getProfile().save();
				getProfile().removePlayerFromWorld();
			}

			try
			{
				if (inputDataStream != null)
					inputDataStream.close();
				
				if (sendQueue != null)
					sendQueue.kill();

				//Null-out to help server gc faster ?
				sendQueue = null;
				inputDataStream = null;
			}
			catch (Exception e)
			{

			}

			//Remove the connection from the set in the manager
			this.connectionsManager.removeDeadConnection(this);
		}
	}

	public void sendChat(String msg)
	{
		sendInternalTextMessage("chat/" + msg);
	}

	void sendInternalTextMessage(String msg)
	{
		// Text flag
		PacketText packet = new PacketText();
		packet.text = msg;
		pushPacket(packet);
	}

	public void pushPacket(Packet packet)
	{
		if (sendQueue != null)
			sendQueue.queue(packet);
	}

	public void flush()
	{
		if (sendQueue != null)
			sendQueue.flush();
	}

	/**
	 * Called after the login token has been checked, or in the case of an offline-mode server, after the client requested to login.
	 */
	private void afterLoginValidation()
	{
		//Prevent user from logging in from two places
		UserConnection contender = this.connectionsManager.getAuthentificatedClientByName(name);
		if (contender != null)
		{
			//SECURITY: this allows someone with one username's credentials to obtain his ip.
			//TBH I'm not too worried about it, if they have your logins they probably can pwn you 10 different ways.
			disconnect("You are already logged in. (" + contender + "). ");
			return;
		}

		//Creates a player based on the thrusted login information
		ServerPlayer player = new ServerPlayer(this);
		setProfile(player);

		//This changes the destinator from a ServerClient to a ServerPlayer, letting know outgoing packets and especially entity components about all the
		//specifics of the player : name, entity he subscribed to, etc
		this.sendQueue.setDestinator(this.getProfile());
		this.sender = this.getProfile();
		
		//Change the packet processor to reflect that ( when receiving packets we have to consider that they are from a player )
		this.packetsProcessor = this.packetsProcessor.toPlayer(player);

		//Fire the login event
		PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(getProfile());
		connectionsManager.getServer().getPluginManager().fireEvent(playerConnectionEvent);
		boolean allowPlayerIn = !playerConnectionEvent.isCancelled();
		//Do we allow him in ?
		if (!allowPlayerIn)
		{
			disconnect(playerConnectionEvent.getRefusedConnectionMessage());
			return;
		}

		//Announce player login
		connectionsManager.getServer().getHandler().broadcastChatMessage(playerConnectionEvent.getConnectionMessage());
		//Aknowledge the login
		sendInternalTextMessage("login/ok");

		//Fluff
		this.setName(this.toString());
	}

	@Override
	public void handleHttpRequest(String tag, String result)
	{
		if (tag.equals("checktoken"))
		{
			if (result.equals("ok"))
				afterLoginValidation();
			else
				disconnect("Invalid session id !");
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (o != null && o instanceof UserConnection)
		{
			UserConnection c = (UserConnection) o;
			if (c.name.equals(name) && socketPort == c.socketPort)
				return true;
		}
		return false;
	}

	public boolean isAuthentificated()
	{
		return getProfile() != null;
	}

	public ServerPlayer getProfile()
	{
		return player;
	}

	public PacketsProcessor getPacketsProcessor()
	{
		return packetsProcessor;
	}

	/**
	 * Called once the user has providen valid authentification
	 */
	private final void setProfile(ServerPlayer profile)
	{
		this.player = profile;
	}

	public void disconnect()
	{
		disconnect("Unknown reason");
	}

	public void disconnect(String disconnectionReason)
	{
		logger.info("Disconnecting client " + this + " for reason " + disconnectionReason);
		sendInternalTextMessage("disconnect/" + disconnectionReason);
		closeSocket();
	}

	public String toString()
	{
		if (this.calledCloseSockedOnce.get())
			return "[Zombie connection !]";

		if (isAuthentificated())
			return "[Connected user '" + getProfile().getName() + "' from " + this.getIp() + "]";
		else
			return "[Unknown connection from " + this.getIp() + "]";
	}

	public Server getServer()
	{
		return connectionsManager.getServer();
	}
}
