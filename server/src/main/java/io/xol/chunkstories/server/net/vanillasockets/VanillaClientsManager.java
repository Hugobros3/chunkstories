//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.vanillasockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.net.ClientConnection;
import io.xol.chunkstories.server.net.ClientsManager;

/**
 * Accepts, manages and destroy player connections
 */
public class VanillaClientsManager extends ClientsManager {

	public VanillaClientsManager(DedicatedServer server) {
		super(server);
	}

	private AtomicBoolean openOnce = new AtomicBoolean(false);
	private AtomicBoolean closeOnce = new AtomicBoolean(false);

	private SocketThread socketThread;

	class SocketThread extends Thread {
		private ServerSocket serverSocket;

		public SocketThread(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
		}

		@Override
		public void run() {
			this.setName("ServerConnectionsManager");

			while (!closeOnce.get()) {
				try {
					Socket childrenSocket = serverSocket.accept();

					ClientConnection clientConnection;
					try {
						clientConnection = new SocketedClientConnection(server, VanillaClientsManager.this,
								childrenSocket);

						// Check for banned ip
						if (server.getUserPrivileges().isIpBanned(clientConnection.getRemoteAddress()))
							clientConnection.disconnect("Banned IP address - " + clientConnection.getRemoteAddress());
						// Check if too many connected users
						else if (clients.size() > maxClients)
							clientConnection.disconnect("Server is full");
						else
							clients.add(clientConnection);
					}
					// Discard failures
					catch (IOException e) {

					}
				} catch (SocketException e) {
					if (!closeOnce.get())
						e.printStackTrace();
				} catch (IOException e) {
					server.logger().error("An unexpected error happened during network stuff. More worldInfo below.");
					e.printStackTrace();
				}
			}
		}

		public void close() throws IOException {
			serverSocket.close();
		}
	}

	@Override
	public boolean open() {
		if (!openOnce.compareAndSet(false, true))
			return false;

		try {
			ServerSocket serverSocket = new ServerSocket(server.getServerConfig().getIntValue("server.net.port"));
			server.logger().info(
					"Started server on port " + serverSocket.getLocalPort() + ", ip=" + serverSocket.getInetAddress());

			socketThread = new SocketThread(serverSocket);
			socketThread.start();

			return true;
		} catch (IOException e) {
			server.logger().error(
					"Can't open server socket. Double check that there is no other instance already running or an application using server port.");
			System.exit(-1);
		}

		return false;
	}

	@Override
	public boolean close() {
		if (closeOnce.compareAndSet(false, true)) {
			try {
				if (socketThread != null) {
					socketThread.close();
					return true;
				}
			} catch (IOException e) {
				server.logger().error("An unexpected error happened during network stuff. More worldInfo below.");
				e.printStackTrace();
			}
		}
		return false;
	}

	public void removeDeadConnection(ClientConnection serverClient) {
		if (clients.contains(serverClient))
			clients.remove(serverClient);
	}
}
