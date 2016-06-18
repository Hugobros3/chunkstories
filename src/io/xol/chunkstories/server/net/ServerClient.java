package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.core.events.PlayerLoginEvent;
import io.xol.chunkstories.core.events.PlayerLogoutEvent;
import io.xol.chunkstories.net.SendQueue;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.PacketText;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerClient extends Thread implements HttpRequester, PacketDestinator, PacketSender
{
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

	ServerClient(Socket s)
	{
		packetsProcessor = new PacketsProcessor(this);
		socket = s;
		socketPort = s.getPort();
		this.setName("Client thread on port" + socketPort);
	}

	// Here's the usefull things !

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
			if (Server.getInstance().serverConfig.getProp("check-version", "true").equals("true"))
			{
				if (Integer.parseInt(version) != VersionInfo.protocolVersion)
					Server.getInstance().handler.disconnectClient(this, "Wrong protocol version ! " + version + " != " + VersionInfo.protocolVersion +" \n Update your game !");
			}
		}
		if (m.startsWith("confirm"))
		{
			if (name.equals("undefined"))
				return;
			if (UsersPrivileges.isUserBanned(name))
			{
				Server.getInstance().handler.disconnectClient(this, "Banned username - " + name);
				return;
			}
			if (token.length() != 20)
			{
				Server.getInstance().handler.disconnectClient(this, "No valid token supplied");
				return;
			}
			if (Server.getInstance().serverConfig.getIntProp("offline-mode", "0") == 1)
			{
				// Offline-mode !
				System.out.println("Warning : Offline-mode is on, letting " + this.name + " connecting without verification");
				
				postTokenCheck();
			}
			else
				new HttpRequestThread(this, "checktoken", "http://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token).start();
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
				ChunkStoriesLogger.getInstance().info("Disconnected "+this+" for causing an "+e.getClass().getSimpleName());
				e.printStackTrace();
				Server.getInstance().handler.disconnectClient(this, "Exception : "+e.getMessage());
			}
			catch (IOException e)
			{
				if(this.isAuthentificated())
					ChunkStoriesLogger.getInstance().info("Connection lost to "+this.getProfile().getDisplayName()+" ("+this.getName()+").");
				
				Server.getInstance().handler.disconnectClient(this, "");
			}
			catch(Exception e)
			{
				ChunkStoriesLogger.getInstance().info("Disconnected "+this+" for causing an "+e.getClass().getSimpleName());
				e.printStackTrace();
				Server.getInstance().handler.disconnectClient(this, "Exception : "+e.getMessage());
			}
		}
		Server.getInstance().handler.disconnectClient(this);
	}

	public String getIp()
	{
		return socket.getInetAddress().getHostAddress();
	}

	public String getHost()
	{
		return socket.getInetAddress().getHostName();
	}

	public void open()
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

	public void close()
	{
		if (alreadyKilled)
			return;
		died = true;
		if (getProfile() != null)
		{
			//authentificated = true;
			//profile = new ServerPlayer(this);
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(getProfile());
			Server.getInstance().getPluginsManager().fireEvent(playerDisconnectionEvent);

			Server.getInstance().handler.sendAllChat(playerDisconnectionEvent.getLogoutMessage());

			//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ") left.");
			assert getProfile() != null;
			getProfile().destroy();
			getProfile().save();
		}

		try
		{
			if (in != null)
				in.close();
			sendQueue.kill();
		}
		catch (Exception e)
		{
		}
		alreadyKilled = true;
	}

	public void sendChat(String msg)
	{
		send("chat/" + msg);
	}

	public void send(String msg)
	{
		// Text flag
		PacketText packet = new PacketText(false);
		packet.text = msg;
		sendPacket(packet);
	}

	public void sendPacket(Packet packet)
	{
		sendQueue.queue(packet);
	}

	/**
	 * Called after the login token has been checked, or in the case of an offline-mode server, after the client requested to login.
	 */
	private void postTokenCheck()
	{
		setProfile(new ServerPlayer(this));
		//This changes the destinator from a ServerClient to a ServerPlayer, letting know outgoing packets and especially entity components about all the
		//specifics of the player : name, entity he subscribed to, etc
		this.sendQueue.setDestinator(this.getProfile());
		this.sender = this.getProfile();
		
		//Fire the login event
		PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(getProfile());
		Server.getInstance().getPluginsManager().fireEvent(playerConnectionEvent);
		boolean allowPlayerIn = !playerConnectionEvent.isCancelled();
		//Do we allow him in ?
		if (!allowPlayerIn)
		{
			Server.getInstance().handler.disconnectClient(this, playerConnectionEvent.getRefusedConnectionMessage());
			return;
		}
		//Announce
		Server.getInstance().handler.sendAllChat(playerConnectionEvent.getConnectionMessage());
		//Aknowledge the login
		send("login/ok");
	}
	
	@Override
	public void handleHttpRequest(String tag, String result)
	{
		if (tag.equals("checktoken"))
		{
			if (result.equals("ok"))
				postTokenCheck();
			else
				Server.getInstance().handler.disconnectClient(this, "Invalid session id !");
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o != null && o instanceof ServerClient)
		{
			ServerClient c = (ServerClient)o;
			if(c.name.equals(name) && socketPort == c.socketPort)
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
}
