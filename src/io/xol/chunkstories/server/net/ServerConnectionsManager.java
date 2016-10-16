package io.xol.chunkstories.server.net;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.net.packets.PacketFile;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.UsersPrivileges;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.net.HttpRequests;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Accepts, manages and destroy player connections
 */
public class ServerConnectionsManager extends Thread
{
	private Server server;
	
	public ServerConnectionsManager(Server server)
	{
		this.server = server;
	}
	
	private AtomicBoolean running = new AtomicBoolean(true);

	private ServerSocket serverSocket;
	
	private Set<ServerClient> clients = ConcurrentHashMap.newKeySet();
	private int maxClients = Server.getInstance().getServerConfig().getIntProp("maxusers", "100");

	private String hostname = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?host=1", "");
	private static String externalIP = "none";

	@Override
	public void start()
	{
		try
		{
			serverSocket = new ServerSocket(Server.getInstance().getServerConfig().getIntProp("server-port", "30410"));
			Server.getInstance().getLogger().info("Started server on port " + serverSocket.getLocalPort() + ", ip=" + serverSocket.getInetAddress());
			
			externalIP = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?ip=1", "");// serverSocket.getInetAddress().getHostAddress();
			super.start();
		}
		catch (IOException e)
		{
			Server.getInstance().getLogger().error("Can't open server socket. Double check that there is no other instance already running or an application using server port.");
			System.exit(-1);
		}

		this.setName("ServerConnectionsManager");
	}

	public void closeConnection()
	{
		running.set(false);
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			Server.getInstance().getLogger().error("An unexpected error happened during network stuff. More info below.");
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		while (running.get())
		{
			try
			{
				Socket sock = serverSocket.accept();
				acceptConnection(new ServerClient(this, sock));
			}
			catch(SocketException e)
			{
				if(running.get())
					e.printStackTrace();
			}
			catch (IOException e)
			{
				Server.getInstance().getLogger().error("An unexpected error happened during network stuff. More info below.");
				e.printStackTrace();
			}
		}
	}

	//TODO move to ServerClient
	public void handle(ServerClient client, String in)
	{
		// Non-login mandatory requests
		if (in.startsWith("login/"))
			client.handleLogin(in.substring(6, in.length()));
		else if (in.startsWith("info"))
			sendServerIntel(client);
		else if(in.equals("mods"))
			client.sendInternalTextMessage("info/mods:"+server.getModsProvider().getModsString());
		else if(in.equals("icon-file"))
		{
			PacketFile iconPacket = new PacketFile(false);
			iconPacket.file = new File("server-icon.png");
			iconPacket.fileTag = "server-icon";
			client.pushPacket(iconPacket);
		}
		// Checks for auth
		if (!client.isAuthentificated())
			return;
		
		// Login-mandatory requests ( you need to be authentificated to use them )
		if (in.equals("co/off"))
		{
			client.closeSocket();
			clients.remove(client);
		}
		else if (in.startsWith("send-mod/")) //md5:
		{
			String modDescriptor = in.substring(9);
			
			String md5 = modDescriptor.substring(4);
			System.out.println(client + " asked for "+md5);
			
			//Give him what he asked for.
			File found = server.getModsProvider().obtainModRedistribuable(md5);
			if(found == null)
			{
				System.out.println("Though luck !");
			}
			else
			{
				System.out.println("Pushing mod md5 "+md5 + "to user.");
				PacketFile iconPacket = new PacketFile(false);
				iconPacket.file = found;
				iconPacket.fileTag = modDescriptor;
				client.pushPacket(iconPacket);
			}
		}
		else if (in.startsWith("world/"))
		{
			Server.getInstance().getWorld().handleWorldMessage(client, in.substring(6, in.length()));
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
					args = chatMsg.substring(chatMsg.indexOf(" ")+1, chatMsg.length()).split(" ");
				}
				
				server.getConsole().dispatchCommand(client.getProfile(), cmdName, args);
			}
			else if (chatMsg.length() > 0)
			{
				sendAllChat(client.getProfile().getDisplayName() + " > " + chatMsg);
			}
		}
	}

	/**
	 * Sends general information about the server
	 * @param client
	 */
	//TODO move to ServerClient
	private void sendServerIntel(ServerClient client)
	{
		client.sendInternalTextMessage("info/name:" + Server.getInstance().getServerConfig().getProp("server-name", "unnamedserver@" + hostname));
		client.sendInternalTextMessage("info/motd:" + Server.getInstance().getServerConfig().getProp("server-desc", "Default description."));
		client.sendInternalTextMessage("info/connected:" + Server.getInstance().getHandler().getNumberOfAuthentificatedClients() + ":" + maxClients);
		client.sendInternalTextMessage("info/version:" + VersionInfo.version);
		client.sendInternalTextMessage("info/mods:"+server.getModsProvider().getModsString());
		client.sendInternalTextMessage("info/done");
		client.flush();
	}

	public void sendAllChat(String chat)
	{
		Server.getInstance().getLogger().info(ColorsTools.convertToAnsi(chat));
		sendAllRaw("chat/" + chat);
	}

	public void sendAllRaw(String raw)
	{
		for (ServerClient client : clients)
		{
			if (client.isAuthentificated())
				client.sendInternalTextMessage(raw);
		}
	}
	
	public void flushAll()
	{
		for (ServerClient client : clients)
			client.flush();
	}

	private void acceptConnection(ServerClient serverClient)
	{
		serverClient.openSocket();
		serverClient.start();
		clients.add(serverClient);
		// Check for banned ip
		if (UsersPrivileges.isIpBanned(serverClient.getIp()))
			serverClient.disconnect("Banned IP address - " + serverClient.getIp());
		// Check if too many connected users
		if( clients.size() > maxClients)
			serverClient.disconnect("Server is full");
	}

	public void removeDeadConnection(ServerClient serverClient)
	{
		if(clients.contains(serverClient))
			clients.remove(serverClient);
	}

	public void closeAll()
	{
		Iterator<ServerClient> clientsIterator = getAllConnectedClients();
		while(clientsIterator.hasNext())
		{
			ServerClient client = clientsIterator.next();
			//Remove the client first to avoid concurrent mod exception
			clientsIterator.remove();
			client.disconnect("Server is closing.");
		}
	}

	public int getMaxClients()
	{
		return maxClients;
	}

	public int getNumberOfAuthentificatedClients()
	{
		int count = 0;
		for (ServerClient c : clients)
		{
			if (c.isAuthentificated())
				count++;
		}
		return count;
	}

	public ServerClient getAuthentificatedClientByName(String name)
	{
		for (ServerClient c : clients)
		{
			if (c.isAuthentificated())
			{
				if (c.name.equals(name))
					return c;
			}
		}
		return null;
	}

	/**
	 * Returns an iterator that only gives clients that have a valid profile and are loaded.
	 * @return
	 */
	public Iterator<ServerClient> getAuthentificatedClients()
	{
		return new Iterator<ServerClient>() {

			Iterator<ServerClient> allClients = clients.iterator();
			ServerClient authentificatedClient = null;
			
			//Finds the next client possible
			private void findNextAuthentificatedClient()
			{
				while(authentificatedClient == null && allClients.hasNext())
				{
					ServerClient nextClient = allClients.next();
					if(nextClient.isAuthentificated())
						authentificatedClient = nextClient;
				}
			}
			
			@Override
			//If no client can be found, then it's the end
			public boolean hasNext()
			{
				if(authentificatedClient == null)
					findNextAuthentificatedClient();
				return authentificatedClient != null;
			}

			@Override
			//Finds a client if none is ready, returns it after having removed it's reference
			public ServerClient next()
			{
				if(authentificatedClient == null)
					findNextAuthentificatedClient();
				
				ServerClient client = authentificatedClient;
				authentificatedClient = null;
				
				return client;
			}
		};
	}
	
	public Iterator<ServerClient> getAllConnectedClients()
	{
		return clients.iterator();
	}

	public String getIP()
	{
		return externalIP;
	}
}
