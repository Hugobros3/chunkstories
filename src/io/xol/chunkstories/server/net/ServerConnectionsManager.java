package io.xol.chunkstories.server.net;

import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.UsersPrivileges;
import io.xol.chunkstories.server.net.ServerToClientConnection.FailedToConnectionException;
import io.xol.engine.misc.ColorsTools;
import io.xol.engine.net.HttpRequests;

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
		this.maxClients = server.getServerConfig().getIntProp("maxusers", "100");
	}

	public Server getServer()
	{
		return server;
	}

	private AtomicBoolean wasCloseConnectionsCalled = new AtomicBoolean(false);

	private ServerSocket serverSocket;

	private Set<ServerToClientConnection> clients = ConcurrentHashMap.newKeySet();
	private int maxClients;

	private String hostname = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?host=1", "");
	private static String externalIP = "none";

	@Override
	public void start()
	{
		try
		{
			serverSocket = new ServerSocket(server.getServerConfig().getIntProp("server-port", "30410"));
			server.getLogger().info("Started server on port " + serverSocket.getLocalPort() + ", ip=" + serverSocket.getInetAddress());

			externalIP = HttpRequests.sendPost("http://chunkstories.xyz/api/sayMyName.php?ip=1", "");// serverSocket.getInetAddress().getHostAddress();
			super.start();
		}
		catch (IOException e)
		{
			server.getLogger().error("Can't open server socket. Double check that there is no other instance already running or an application using server port.");
			System.exit(-1);
		}

		this.setName("ServerConnectionsManager");
	}

	public void closeConnectionsManager()
	{
		if (wasCloseConnectionsCalled.compareAndSet(false, true))
		{
			try
			{
				serverSocket.close();
			}
			catch (IOException e)
			{
				server.getLogger().error("An unexpected error happened during network stuff. More info below.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run()
	{
		while (!wasCloseConnectionsCalled.get())
		{
			try
			{
				Socket sock = serverSocket.accept();

				ServerToClientConnection clientConnection;
				try
				{
					clientConnection = new ServerToClientConnection(this, sock);
					clients.add(clientConnection);
					// Check for banned ip
					if (UsersPrivileges.isIpBanned(clientConnection.getIp()))
						clientConnection.disconnect("Banned IP address - " + clientConnection.getIp());
					// Check if too many connected users
					if (clients.size() > maxClients)
						clientConnection.disconnect("Server is full");
				}
				//Discard failures
				catch (FailedToConnectionException e)
				{
				}
			}
			catch (SocketException e)
			{
				if (!wasCloseConnectionsCalled.get())
					e.printStackTrace();
			}
			catch (IOException e)
			{
				server.getLogger().error("An unexpected error happened during network stuff. More info below.");
				e.printStackTrace();
			}
		}
	}

	public void broadcastChatMessage(String chat)
	{
		server.getLogger().info(ColorsTools.convertToAnsi(chat));
		for (ServerToClientConnection client : clients)
		{
			if (client.isAuthentificated())
				client.sendInternalTextMessage("chat/" + chat);
		}
	}

	public void flushAll()
	{
		for (ServerToClientConnection client : clients)
			client.flush();
	}

	void removeDeadConnection(ServerToClientConnection serverClient)
	{
		if (clients.contains(serverClient))
			clients.remove(serverClient);
	}

	public void closeAll()
	{
		Iterator<ServerToClientConnection> clientsIterator = getAllConnectedClients();
		while (clientsIterator.hasNext())
		{
			ServerToClientConnection client = clientsIterator.next();
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
		for (ServerToClientConnection c : clients)
		{
			if (c.isAuthentificated())
				count++;
		}
		return count;
	}

	public ServerToClientConnection getAuthentificatedClientByName(String name)
	{
		for (ServerToClientConnection c : clients)
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
	 * 
	 * @return
	 */
	public Iterator<ServerToClientConnection> getAuthentificatedClients()
	{
		return new Iterator<ServerToClientConnection>()
		{

			Iterator<ServerToClientConnection> allClients = clients.iterator();
			ServerToClientConnection authentificatedClient = null;

			//Finds the next client possible
			private void findNextAuthentificatedClient()
			{
				while (authentificatedClient == null && allClients.hasNext())
				{
					ServerToClientConnection nextClient = allClients.next();
					if (nextClient.isAuthentificated())
						authentificatedClient = nextClient;
				}
			}

			@Override
			//If no client can be found, then it's the end
			public boolean hasNext()
			{
				if (authentificatedClient == null)
					findNextAuthentificatedClient();
				return authentificatedClient != null;
			}

			@Override
			//Finds a client if none is ready, returns it after having removed it's reference
			public ServerToClientConnection next()
			{
				if (authentificatedClient == null)
					findNextAuthentificatedClient();

				ServerToClientConnection client = authentificatedClient;
				authentificatedClient = null;

				return client;
			}
		};
	}

	public Iterator<ServerToClientConnection> getAllConnectedClients()
	{
		return clients.iterator();
	}

	public String getIP()
	{
		return externalIP;
	}

	public String getHostname()
	{
		return hostname;
	}
}
