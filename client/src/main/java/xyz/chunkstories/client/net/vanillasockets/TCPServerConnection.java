//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net.vanillasockets;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.exceptions.net.UnknowPacketException;
import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.PacketDefinition.PacketGenre;
import xyz.chunkstories.api.net.PacketWorldStreaming;
import xyz.chunkstories.api.net.RemoteServer;
import xyz.chunkstories.api.net.packets.PacketText;
import xyz.chunkstories.client.ClientImplementation;
import xyz.chunkstories.client.net.ClientPacketsEncoderDecoder;
import xyz.chunkstories.client.net.ConnectionStep;
import xyz.chunkstories.client.net.RemoteServerImplementation;
import xyz.chunkstories.client.net.ServerConnection;
import xyz.chunkstories.net.Connection;
import xyz.chunkstories.net.LogicalPacketDatagram;
import xyz.chunkstories.net.PacketDefinitionImplementation;
import xyz.chunkstories.net.vanillasockets.SendQueue;
import xyz.chunkstories.net.vanillasockets.StreamGobbler;
import xyz.chunkstories.world.WorldClientRemote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** A clientside connection to a server using the TCP protocol. */
public class TCPServerConnection extends ServerConnection {
	private final ClientImplementation client;
	private final ClientPacketsEncoderDecoder encoderDecoder;

	private Socket socket = null;

	private boolean connected = false, disconnected = false;

	private AtomicBoolean connectOnce = new AtomicBoolean(false);
	private AtomicBoolean closeOnce = new AtomicBoolean(false);

	private SendQueue sendQueue;

	// A representation of who we're talking to
	private RemoteServer remoteServer;

	public TCPServerConnection(ClientImplementation client, String remoteAddress, int port) {
		super(remoteAddress, port);

		this.client = client;
		remoteServer = new RemoteServerImplementation(this);
		encoderDecoder = new ClientPacketsEncoderDecoder(client, this);
	}

	@Override
	public boolean connect() {
		if (!connectOnce.compareAndSet(false, true))
			return false;

		try {
			socket = new Socket(remoteAddress, port);

			DataInputStream in = new DataInputStream(socket.getInputStream());
			StreamGobbler streamGobbler = new ClientGobbler(this, in);

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
	public void handleDatagram(LogicalPacketDatagram datagram)
			throws IOException, PacketProcessingException, IllegalPacketException {
		PacketDefinitionImplementation definition = (PacketDefinitionImplementation) datagram.packetDefinition;// (PacketDefinitionImpl)
																												// getEncoderDecoder().getContentTranslator().getPacketForId(datagram.packetTypeId);
		if (definition.getGenre() == PacketGenre.GENERAL_PURPOSE) {
			Packet packet = definition.createNew(true, null);
			packet.process(getRemoteServer(), datagram.getData(), getEncoderDecoder());
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.SYSTEM) {
			Packet packet = definition.createNew(true, null);
			packet.process(getRemoteServer(), datagram.getData(), getEncoderDecoder());
			if (packet instanceof PacketText) {
				handleSystemRequest(((PacketText) packet).text);
			}
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.WORLD) {
			WorldClientRemote world = getEncoderDecoder().getWorld();
			world.queueDatagram(datagram);

		} else if (definition.getGenre() == PacketGenre.WORLD_STREAMING) {
			WorldClientRemote world = getEncoderDecoder().getWorld();
			PacketWorldStreaming packet = (PacketWorldStreaming) definition.createNew(true, world);
			packet.process(getRemoteServer(), datagram.getData(), getEncoderDecoder());
			world.ioHandler().handlePacketWorldStreaming(packet);
			datagram.dispose();
		} else {
			throw new RuntimeException("whut");
		}
	}

	public boolean handleSystemRequest(String msg) {
		if (msg.startsWith("chat/")) {
			client.print(msg.substring(5, msg.length()));
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
			sendQueue.queue(encoderDecoder.buildOutgoingPacket(packet));
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
	public ClientPacketsEncoderDecoder getEncoderDecoder() {
		return this.encoderDecoder;
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
