package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.tech.ServerConsole;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.net.HttpRequests;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerConnectionsHandler extends Thread
{
	AtomicBoolean running = new AtomicBoolean(true);

	// network aspect
	ServerSocket serverSocket;
	public List<ServerClient> clients = new ArrayList<ServerClient>();
	public static String ip = "none";

	public int maxClients = Server.getInstance().serverConfig.getIntProp("maxusers", "100");

	String hostname = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?host=1", "");

	public void start()
	{
		try
		{
			serverSocket = new ServerSocket(Server.getInstance().serverConfig.getIntProp("server-port", "30410"));
			Server.getInstance().log.info("Started server on port " + serverSocket.getLocalPort() + ", ip=" + serverSocket.getInetAddress());
			ip = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?ip=1", "");// serverSocket.getInetAddress().getHostAddress();
			super.start();
		}
		catch (IOException e)
		{
			Server.getInstance().log.severe("Can't open server socket. Double check that there is no other instance already running or an application using server port.");
			System.exit(-1);
		}

		this.setName("Server Connections Main Thread");
	}

	public void close()
	{
		running.set(false);
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			Server.getInstance().log.severe("An unexpected error happened during network stuff. More info below.");
			e.printStackTrace();
		}
	}

	public void run()
	{
		while (running.get())
		{
			try
			{
				Socket sock = serverSocket.accept();
				// Server.log.info("Accepted connection from "+sock.getInetAddress().getHostAddress()+":"+sock.getLocalPort());
				addClient(new ServerClient(sock));
			}
			catch(SocketException e)
			{
				if(running.get())
					e.printStackTrace();
			}
			catch (IOException e)
			{
				Server.getInstance().log.severe("An unexpected error happened during network stuff. More info below.");
				e.printStackTrace();
			}
		}
	}

	public void handle(ServerClient c, String in)
	{
		// Non-login mandatory requests
		if (in.startsWith("login/"))
			c.handleLogin(in.substring(6, in.length()));
		else if (in.startsWith("info"))
			sendIntel(c);
		// Checks for auth
		if (!c.authentificated)
			return;
		// Any authentificated client has to have a profile.
		assert c.profile != null;
		// Login-mandatory requests ( you need to be authentificated to use them )
		if (in.equals("co/off"))
		{
			c.close();
			clients.remove(c);
		}
		else if (in.startsWith("world/"))
		{
			Server.getInstance().world.handleWorldMessage(c, in.substring(6, in.length()));
		}
		else if (in.startsWith("chat/"))
		{
			String chatMsg = in.substring(5, in.length());
			if (chatMsg.startsWith("/"))
			{
				ServerConsole.handleCommand(chatMsg.substring(1, chatMsg.length()), c.profile);
			}
			else if (chatMsg.length() > 0)
			{
				sendAllChat(c.profile.getDisplayName() + " > " + chatMsg);
			}
		}
		// Debug
		// System.out.println(ColorsTools.convertToAnsi("Client " + c.getHost() + " sent " + in));
	}

	private void sendIntel(ServerClient c)
	{
		c.send("info/name:" + Server.getInstance().serverConfig.getProp("server-name", "unnamedserver@" + hostname));
		c.send("info/motd:" + Server.getInstance().serverConfig.getProp("server-desc", "Default description."));
		c.send("info/connected:" + Server.getInstance().handler.getNumberOfConnectedClients() + ":" + maxClients);
		c.send("info/version:" + VersionInfo.version);
		c.send("info/nogame");
		c.send("info/done");
		// disconnectClient(c, "???");
	}

	public void sendAllChat(String chat)
	{
		Server.getInstance().log.info(ColorsTools.convertToAnsi(chat));
		sendAllRaw("chat/" + chat);
	}

	public void sendAllRaw(String raw)
	{
		for (ServerClient c : clients)
		{
			if (c.authentificated)
				c.send(raw);
		}
	}

	public void addClient(ServerClient serverClient)
	{
		serverClient.open();
		serverClient.start();
		clients.add(serverClient);

		// Check for banned ip
		if (UsersPrivileges.isIpBanned(serverClient.getIp()))
			disconnectClient(serverClient, "Banned IP address - " + serverClient.getIp());
		// Check if too many connected users
		if( clients.size() > maxClients)
			disconnectClient(serverClient, "Server is full");
	}

	public void disconnectClient(ServerClient serverClient, String message)
	{
		serverClient.send("disconnect/" + message);
		disconnectClient(serverClient);
	}

	public void disconnectClient(ServerClient serverClient)
	{
		serverClient.close();
		// serverClient.stop();
		clients.remove(serverClient);
	}

	public void disconnectClientByIp(String ip)
	{
		ServerClient c = null;
		for (ServerClient sc : clients)
		{
			if (sc.getIp().equals(ip))
				c = sc;
		}
		if (c != null)
			disconnectClient(c);
	}

	public void closeAll()
	{
		List<ServerClient> disconnectEm = new ArrayList<ServerClient>();
		for (ServerClient sc : clients)
		{
			disconnectEm.add(sc);
		}
		for (ServerClient sc : disconnectEm)
		{
			disconnectClient(sc, "Server Closing");
		}
	}

	public int getNumberOfConnectedClients()
	{
		int i = 0;
		for (ServerClient c : clients)
		{
			if (c.authentificated)
				i++;
		}
		return i;
	}

	public ServerClient getClientByName(String name)
	{
		ServerClient him = null;
		for (ServerClient c : clients)
		{
			if (c.authentificated)
			{
				if (c.name.equals(name))
					him = c;
			}
		}
		return him;
	}

	public List<ServerClient> getAuthentificatedClients()
	{
		List<ServerClient> authClients = new ArrayList<ServerClient>();

		for (ServerClient c : clients)
		{
			if (c.authentificated)
			{
				authClients.add(c);
			}
		}
		
		return authClients;
	}
}
