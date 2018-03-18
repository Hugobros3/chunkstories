//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net.vanillasockets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.client.net.ClientPacketsContext;
import io.xol.chunkstories.client.net.ConnectionStep;
import io.xol.chunkstories.client.net.RemoteServerImplementation;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.LogicalPacketDatagram;
import io.xol.chunkstories.net.PacketDefinitionImplementation;
import io.xol.chunkstories.net.vanillasockets.SendQueue;
import io.xol.chunkstories.net.vanillasockets.StreamGobbler;
import io.xol.chunkstories.world.WorldClientRemote;

/** A clientside connection to a server using the TCP protocol. */
public class TCPServerConnection extends ServerConnection {

	private final ClientInterface client;
	private final ClientPacketsContext packetsContext;

	private Socket socket = null;
	private DataInputStream in = null;

	private boolean connected = false, disconnected = false;

	private AtomicBoolean connectOnce = new AtomicBoolean(false);
	private AtomicBoolean closeOnce = new AtomicBoolean(false);

	private StreamGobbler streamGobbler;
	private SendQueue sendQueue;

	// A representation of who we're talking to
	private RemoteServer remoteServer;

	public TCPServerConnection(ClientInterface gameContext, String remoteAddress, int port) {
		super(gameContext, remoteAddress, port);
		this.client = gameContext;

		packetsContext = new ClientPacketsContext(gameContext, this);
		remoteServer = new RemoteServerImplementation(this);
	}

	@Override
	public boolean connect() {
		if (!connectOnce.compareAndSet(false, true))
			return false;

		try {
			socket = new Socket(remoteAddress, port);
			
			in = new DataInputStream(socket.getInputStream());
			streamGobbler = new ClientGobbler(this, in);

			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			sendQueue = new SendQueue(this, out);

			connected = true;
			
			streamGobbler.start();
			sendQueue.start();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	class ClientGobbler extends StreamGobbler {

		public ClientGobbler(Connection connection, DataInputStream in) {
			super(connection, in);
		}
	}

	@Override
	public void handleDatagram(LogicalPacketDatagram datagram) throws IOException, PacketProcessingException, IllegalPacketException {
		PacketDefinitionImplementation definition = (PacketDefinitionImplementation) datagram.packetDefinition;//(PacketDefinitionImpl) getPacketsContext().getContentTranslator().getPacketForId(datagram.packetTypeId);
		if (definition.getGenre() == PacketGenre.GENERAL_PURPOSE) {
			Packet packet = definition.createNew(true, null);
			packet.process(getRemoteServer(), datagram.getData(), getPacketsContext());
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.SYSTEM) {
			Packet packet = definition.createNew(true, null);
			packet.process(getRemoteServer(), datagram.getData(), getPacketsContext());
			if (packet instanceof PacketText) {
				handleSystemRequest(((PacketText) packet).text);
			}
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.WORLD) {
			WorldClientRemote world = getPacketsContext().getWorld();
			world.queueDatagram(datagram);

		} else if (definition.getGenre() == PacketGenre.WORLD_STREAMING) {
			WorldClientRemote world = getPacketsContext().getWorld();
			PacketWorldStreaming packet = (PacketWorldStreaming) definition.createNew(true, world);
			packet.process(getRemoteServer(), datagram.getData(), getPacketsContext());
			world.ioHandler().handlePacketWorldStreaming(packet);
			datagram.dispose();
		} else {
			throw new RuntimeException("whut");
		}
	}

	public boolean handleSystemRequest(String msg) {
		if (msg.startsWith("chat/")) {
			client.printChat(msg.substring(5, msg.length()));
		} else if (msg.startsWith("disconnect/")) {
			String errorMessage = msg.replace("disconnect/", "");
			logger.info("Disconnected by server : " + errorMessage);
			close();
		}

		return false;
	}

	@Override
	public void pushPacket(Packet packet) {
		try {
			sendQueue.queue(packetsContext.buildOutgoingPacket(packet));
		} catch (UnknowPacketException e) {
			logger.error("Couldn't pushPacket()", e);
		} catch (IOException e) {
			close();
		}
	}

	@Override
	public RemoteServer getRemoteServer() {
		return remoteServer;
	}

	/** Represents the step of downloading a mod from a server */
	class ConnectionStepDownloadMod extends ConnectionStep {

		public ConnectionStepDownloadMod(String text) {
			super(text);
		}

		Semaphore wait = new Semaphore(0);
		DownloadStatus downloadStatus;

		@Override
		public String getStepText() {
			if (downloadStatus == null)
				return super.getStepText();
			return downloadStatus.bytesDownloaded() + " / " + downloadStatus.totalBytes();
		}

		@Override
		public void waitForEnd() {
			wait.acquireUninterruptibly();
			downloadStatus.waitForEnd();
			super.waitForEnd();
		}

		/** Called when we start to receive said mod */
		public void callback(DownloadStatus progress) {
			wait.release();
			downloadStatus = progress;
		}
	}

	@Override
	public ConnectionStep obtainModFile(String modMd5Hash, File cached) {
		ConnectionStepDownloadMod showProgress = new ConnectionStepDownloadMod("Waiting for download response...");

		this.registerExpectedFileStreaming("md5:" + modMd5Hash, cached, (progress) -> showProgress.callback(progress));
		sendTextMessage("send-mod/md5:" + modMd5Hash);

		return showProgress;
	}

	@Override
	public ClientPacketsContext getPacketsContext() {
		return this.packetsContext;
	}

	@Override
	public void flush() {
		if (sendQueue != null)
			sendQueue.flush();
	}

	@Override
	public boolean close() {
		if (!closeOnce.compareAndSet(false, true))
			return false;

		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			// Discard errors when disconnecting a connection
		}

		if (sendQueue != null)
			sendQueue.kill();

		disconnected = true;
		return true;
	}

	@Override
	public boolean isOpen() {
		return connected && !disconnected;
	}

}
