package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.core.events.PlayerLoginEvent;
import io.xol.chunkstories.core.events.PlayerLogoutEvent;
import io.xol.chunkstories.net.SendQueue;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.PacketFile;
import io.xol.chunkstories.net.packets.PacketText;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.UsersPrivileges;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerClient extends Thread implements HttpRequester, PacketDestinator, PacketSender
{
	private final ServerConnectionsManager connectionsManager;

	int socketPort = 0;

	//Streams.
	Socket socket;
	PacketsProcessor packetsProcessor;
	DataInputStream in = null;
	SendQueue sendQueue;

	boolean validToken = false;
	String token = "undefined";

	//Did the connection died at some point ?
	boolean died = false;
	//Used to pevent calling close() twice
	boolean alreadyKilled = false;

	public String name = "undefined";
	//Version of the client he uses
	public String version = "undefined";

	//Assertion : if the player is authentificated it has a profile
	private ServerPlayer profile;

	private PacketSender sender = this;

	ServerClient(ServerConnectionsManager connectionsManager, Socket socket)
	{
		this.connectionsManager = connectionsManager;

		this.socket = socket;
		this.socketPort = socket.getPort();

		this.packetsProcessor = new PacketsProcessor(this);
		this.setName(this.toString());
	}

	// Here's the usefull things !

	public void handle(String in)
	{
		// Non-login mandatory requests
		if (in.startsWith("login/"))
			this.handleLogin(in.substring(6, in.length()));
		else if (in.startsWith("info"))
			this.connectionsManager.sendServerIntel(this);
		else if (in.equals("mods"))
			this.sendInternalTextMessage("info/mods:" + Server.getInstance().getModsProvider().getModsString());
		else if (in.equals("icon-file"))
		{
			PacketFile iconPacket = new PacketFile();
			iconPacket.file = new File("server-icon.png");
			iconPacket.fileTag = "server-icon";
			this.pushPacket(iconPacket);
			this.flush();
		}
		// Checks for auth
		if (!this.isAuthentificated())
			return;

		// Login-mandatory requests ( you need to be authentificated to use them )
		if (in.equals("co/off"))
		{
			this.disconnect("Client terminated connection");
			//clients.remove(client);
		}
		else if (in.startsWith("send-mod/")) //md5:
		{
			String modDescriptor = in.substring(9);

			String md5 = modDescriptor.substring(4);
			System.out.println(this + " asked for " + md5);

			//Give him what he asked for.
			File found = Server.getInstance().getModsProvider().obtainModRedistribuable(md5);
			if (found == null)
			{
				System.out.println("Though luck !");
			}
			else
			{
				System.out.println("Pushing mod md5 " + md5 + "to user.");
				PacketFile modUploadPacket = new PacketFile();
				modUploadPacket.file = found;
				modUploadPacket.fileTag = modDescriptor;
				this.pushPacket(modUploadPacket);
			}
		}
		else if (in.startsWith("world/"))
		{
			Server.getInstance().getWorld().handleWorldMessage(this, in.substring(6, in.length()));
		}
		else if (in.startsWith("chat/"))
		{
			String chatMsg = in.substring(5, in.length());
			//Console commands start with a /
			if (chatMsg.startsWith("/"))
			{
				chatMsg = chatMsg.substring(1, chatMsg.length());

				String cmdName = chatMsg.toLowerCase();
				String[] args = {};
				if (chatMsg.contains(" "))
				{
					cmdName = chatMsg.substring(0, chatMsg.indexOf(" "));
					args = chatMsg.substring(chatMsg.indexOf(" ") + 1, chatMsg.length()).split(" ");
				}

				Server.getInstance().getConsole().dispatchCommand(this.getProfile(), cmdName, args);
			}
			else if (chatMsg.length() > 0)
			{
				connectionsManager.sendAllChat(this.getProfile().getDisplayName() + " > " + chatMsg);
			}
		}
	}

	public void handleLogin(String m)
	{
		if (m.startsWith("username:"))
		{
			this.name = m.replace("username:", "");
		}
		if (m.startsWith("logintoken:"))
		{
			token = m.replace("logintoken:", "");
		}
		if (m.startsWith("version:"))
		{
			version = m.replace("version:", "");
			if (Server.getInstance().getServerConfig().getProp("check-version", "true").equals("true"))
			{
				if (Integer.parseInt(version) != VersionInfo.networkProtocolVersion)
					disconnect("Wrong protocol version ! " + version + " != " + VersionInfo.networkProtocolVersion + " \n Update your game !");
			}
		}
		if (m.startsWith("confirm"))
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
			if (Server.getInstance().getServerConfig().getIntProp("offline-mode", "0") == 1)
			{
				// Offline-mode !
				System.out.println("Warning : Offline-mode is on, letting " + this.name + " connecting without verification");
				afterLoginValidation();
			}
			else
				new HttpRequestThread(this, "checktoken", "http://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token);
		}
	}

	// Just socket bullshit !
	@Override
	public void run()
	{
		while (!died)
		{
			try
			{
				//Process incomming packets
				Packet packet = packetsProcessor.getPacket(in, false);
				packet.process(sender, in, packetsProcessor);
			}
			catch (IllegalPacketException | UnknowPacketException e)
			{
				ChunkStoriesLogger.getInstance().info("Disconnected " + this + " for causing an " + e.getClass().getSimpleName());
				e.printStackTrace();
				disconnect("Exception : " + e.getMessage());
				return;
			}
			catch (IOException e)
			{
				if (this.isAuthentificated())
					ChunkStoriesLogger.getInstance().info("Connection lost to " + this.getProfile().getDisplayName() + " (" + this.getName() + ").");

				disconnect("Broken socket");
				return;
			}
			catch (Exception e)
			{
				ChunkStoriesLogger.getInstance().info("Disconnected " + this + " for causing an " + e.getClass().getSimpleName());
				e.printStackTrace();
				disconnect("Exception : " + e.getMessage());
				return;
			}
		}
		disconnect("Socket terminated");
	}

	public String getIp()
	{
		return socket.getInetAddress().getHostAddress();
	}

	public String getHost()
	{
		return socket.getInetAddress().getHostName();
	}

	public void openSocket()
	{
		try
		{
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

			//The sendQueue is initialized with 'this' as the destinator, 'this', the ServerClient class does not implement the Subscriber interface
			//but provides the PacketDestinator interface so outgoing packets can know what kind of client they are talking to ( in this very case, an unauthentificated client )
			//See : postTokenCheck() for more information
			sendQueue = new SendQueue(this, new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())), packetsProcessor);
			sendQueue.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void closeSocket()
	{
		if (alreadyKilled)
			return;
		died = true;
		if (getProfile() != null)
		{
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(getProfile());
			Server.getInstance().getPluginManager().fireEvent(playerDisconnectionEvent);

			Server.getInstance().getHandler().sendAllChat(playerDisconnectionEvent.getLogoutMessage());

			//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ") left.");
			assert getProfile() != null;
			getProfile().save();
			getProfile().destroy();
		}

		try
		{
			if (in != null)
				in.close();
			if (sendQueue != null)
				sendQueue.kill();

			//Null-out for server gc ?
			sendQueue = null;
			in = null;
		}
		catch (Exception e)
		{

		}
		alreadyKilled = true;
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
		if(sendQueue != null)
			sendQueue.queue(packet);
	}

	public void flush()
	{
		if(sendQueue != null)
			sendQueue.flush();
	}

	/**
	 * Called after the login token has been checked, or in the case of an offline-mode server, after the client requested to login.
	 */
	private void afterLoginValidation()
	{
		//Prevent user from logging in from two places
		ServerClient contender = this.connectionsManager.getAuthentificatedClientByName(name);
		if (contender != null)
		{
			//SECURITY: this allows someone with one username's credentials to obtain his ip.
			//TBH I'm not too worried about it, if they have your logins they probably can pwn you 10 different ways.
			disconnect("You are already logged in. (" + contender + "). ");
			return;
		}

		setProfile(new ServerPlayer(this));

		//This changes the destinator from a ServerClient to a ServerPlayer, letting know outgoing packets and especially entity components about all the
		//specifics of the player : name, entity he subscribed to, etc
		this.sendQueue.setDestinator(this.getProfile());
		this.sender = this.getProfile();

		//Fire the login event
		PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(getProfile());
		Server.getInstance().getPluginManager().fireEvent(playerConnectionEvent);
		boolean allowPlayerIn = !playerConnectionEvent.isCancelled();
		//Do we allow him in ?
		if (!allowPlayerIn)
		{
			disconnect(playerConnectionEvent.getRefusedConnectionMessage());
			return;
		}

		//Announce player login
		Server.getInstance().getHandler().sendAllChat(playerConnectionEvent.getConnectionMessage());
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
		if (o != null && o instanceof ServerClient)
		{
			ServerClient c = (ServerClient) o;
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
		return profile;
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
		this.profile = profile;
	}

	public void disconnect()
	{
		disconnect("Unknown reason");
	}

	public void disconnect(String disconnectionReason)
	{
		//Thread.dumpStack();
		
		ChunkStoriesLogger.getInstance().info("Disconnecting client " + this + " for reason " + disconnectionReason);
		sendInternalTextMessage("disconnect/" + disconnectionReason);
		closeSocket();
		//Remove the connection from the set in the manager
		this.connectionsManager.removeDeadConnection(this);
	}

	public String toString()
	{
		if(died)
			return "[Zombie connection !]";
		
		if (isAuthentificated())
			return "[Connected user '" + getProfile().getName() + "' from " + this.getIp() + "]";
		else
			return "[Unknown connection from " + this.getIp() + "]";
	}
}
