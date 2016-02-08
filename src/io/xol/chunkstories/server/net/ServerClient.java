package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.events.core.PlayerLoginEvent;
import io.xol.chunkstories.api.events.core.PlayerLogoutEvent;
import io.xol.chunkstories.client.net.SendQueue;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.Packet;
import io.xol.chunkstories.net.packets.Packet00Text;
import io.xol.chunkstories.net.packets.Packet01WorldInfo;
import io.xol.chunkstories.net.packets.Packet02ChunkCompressedData;
import io.xol.chunkstories.net.packets.Packet03ChunkSummary;
import io.xol.chunkstories.net.packets.Packet04Entity;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.engine.misc.HttpRequestThread;
import io.xol.engine.misc.HttpRequester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerClient extends Thread implements HttpRequester
{
	Socket sock;
	public int id = 0;

	DataInputStream in = null;
	SendQueue queue;

	boolean validToken = false;
	String token = "undefined";
	public boolean authentificated = false;
	boolean died = false;
	boolean alreadyKilled = false;

	public String name = "undefined";
	public String version = "undefined";
	public ServerPlayer profile;

	ServerClient(Socket s)
	{
		sock = s;
		id = s.getPort();
		this.setName("Client thread " + id);
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
				if (!version.equals(VersionInfo.version))
					Server.getInstance().handler.disconnectClient(this, "Wrong version ! " + version + " != " + VersionInfo.version);
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
				authentificated = true;
				profile = new ServerPlayer(this);
				Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ")" + " joined.");
				send("login/ok");
			}
			else
				new HttpRequestThread(this, "checktoken", "http://chunkstories.xyz/api/serverTokenChecker.php", "username=" + this.name + "&token=" + token).start();
		}
	}

	// Just socket bullshit !
	public void run()
	{
		// Server.getInstance().log.info("Client " + id +
		// " handling thread started properly.");
		while (!died)
		{
			try
			{
				byte type = in.readByte();
				handlePacket(type, in);

				/*if (type == 0x00)
					Server.getInstance().handler.handle(this, in.readUTF());
				else
					handleBinary(type, in);*/
			}
			catch (IOException e)
			{
				died = true;
				System.out.println("Socket " + id + " (" + getIp() + ") died (" + e.getClass().getName() + ")");
			}
			catch (IllegalPacketException | UnknowPacketException e)
			{
				Server.getInstance().handler.disconnectClient(e.getMessage());
			}
		}
		Server.getInstance().handler.disconnectClient(this);
	}

	private void handlePacket(byte type, DataInputStream in) throws IOException, IllegalPacketException, UnknowPacketException
	{
		if (type == 0x00)
		{
			// UTF-8 text data
			Packet00Text packet = new Packet00Text(false);
			packet.read(in);
			Server.getInstance().handler.handle(this, packet.text);
		}
		else if (type == 0x01)
		{
			Packet01WorldInfo packet = new Packet01WorldInfo(false);

			throw new IllegalPacketException(packet);
		}
		else if (type == 0x02)
		{
			Packet02ChunkCompressedData packet = new Packet02ChunkCompressedData(false);

			throw new IllegalPacketException(packet);
		}
		else if (type == 0x03)
		{
			Packet03ChunkSummary packet = new Packet03ChunkSummary(false);

			throw new IllegalPacketException(packet);
		}
		else if (type == 0x04)
		{
			Packet04Entity packet = new Packet04Entity(false);
			packet.read(in);
			if (this.profile.entity != null && packet.entityID == this.profile.entity.entityID)
				packet.applyToEntity(this.profile.entity);
			else
				packet.applyToEntity(null);
			//entity = EntitiesList.newEntity(world, entityType);
		}
		else
		{
			throw new UnknowPacketException(type);
		}
	}

	public String getIp()
	{
		return sock.getInetAddress().getHostAddress();
	}

	public String getHost()
	{
		return sock.getInetAddress().getHostName();
	}

	public void open()
	{
		try
		{
			in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
			queue = new SendQueue(new DataOutputStream(new BufferedOutputStream(sock.getOutputStream())));
			queue.start();
			//out = );
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
		if (authentificated)
		{
			//authentificated = true;
			//profile = new ServerPlayer(this);
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(profile);
			Server.getInstance().getPluginsManager().fireEvent(playerDisconnectionEvent);

			Server.getInstance().handler.sendAllChat(playerDisconnectionEvent.connectionMessage);

			//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ") left.");
			assert profile != null;
			//if (profile != null)
			//{
			profile.onLeave();
			profile.save();
			//}
		}

		try
		{
			if (in != null)
				in.close();
			queue.kill();
			//if (out != null)
			//	out.close();
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
		Packet00Text packet = new Packet00Text(false);
		packet.text = msg;
		sendPacket(packet);
	}

	public void sendPacket(Packet packet)
	{
		queue.queue(packet);
	}

	public void handleHttpRequest(String info, String result)
	{
		if (info.equals("checktoken"))
		{
			if (result.equals("ok"))
			{
				authentificated = true;
				profile = new ServerPlayer(this);
				PlayerLoginEvent playerConnectionEvent = new PlayerLoginEvent(profile);
				boolean allowPlayerIn = Server.getInstance().getPluginsManager().fireEvent(playerConnectionEvent);
				if (!allowPlayerIn)
				{
					Server.getInstance().handler.disconnectClient(this, playerConnectionEvent.refusedConnectionMessage);
					return;
				}
				//System.out.println(allowPlayerIn+"allow");
				Server.getInstance().handler.sendAllChat(playerConnectionEvent.connectionMessage);
				//Server.getInstance().handler.sendAllChat("#FFD000" + name + " (" + getIp() + ")" + " joined.");
				profile.onJoin();
				send("login/ok");
			}
			else
			{
				Server.getInstance().handler.disconnectClient(this, "Invalid session id !");
			}
		}
	}
}
