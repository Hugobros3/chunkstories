//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net.vanillasockets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.exceptions.net.UnknowPacketException;
import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.PacketDefinition.PacketGenre;
import xyz.chunkstories.api.net.PacketWorldStreaming;
import xyz.chunkstories.api.net.packets.PacketText;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.net.Connection;
import xyz.chunkstories.net.LogicalPacketDatagram;
import xyz.chunkstories.net.PacketDefinitionImplementation;
import xyz.chunkstories.net.vanillasockets.SendQueue;
import xyz.chunkstories.net.vanillasockets.StreamGobbler;
import xyz.chunkstories.server.net.ClientConnection;
import xyz.chunkstories.server.net.ClientsManager;
import xyz.chunkstories.world.WorldServer;

public class SocketedClientConnection extends ClientConnection {

	final ClientsManager clientsManager;
	final Socket socket;

	private AtomicBoolean closeOnce = new AtomicBoolean(false);
	private boolean disconnected = false;

	private StreamGobbler streamGobbler;
	private SendQueue sendQueue;

	public SocketedClientConnection(Server server, ClientsManager clientsManager, Socket socket)
			throws IOException {
		super(server, clientsManager, socket.getInetAddress().getHostAddress(), socket.getPort());
		this.clientsManager = clientsManager;
		this.socket = socket;

		// We get exceptions early if this fails
		InputStream socketInputStream = socket.getInputStream();
		OutputStream socketOutputStream = socket.getOutputStream();

		DataInputStream inputDataStream = new DataInputStream(new BufferedInputStream(socketInputStream));
		DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socketOutputStream));

		this.packetsProcessor = clientsManager.getPacketsProcessor().forConnection(this);

		streamGobbler = new ServerClientGobbler(this, inputDataStream);
		streamGobbler.start();

		sendQueue = new SendQueue(this, dataOutputStream);
		sendQueue.start();
	}

	class ServerClientGobbler extends StreamGobbler {

		public ServerClientGobbler(Connection connection, DataInputStream in) {
			super(connection, in);
		}

	}

	@Override
	public void flush() {
		sendQueue.flush();
	}

	@Override
	public void handleDatagram(LogicalPacketDatagram datagram)
			throws IOException, PacketProcessingException, IllegalPacketException {
		PacketDefinitionImplementation definition = (PacketDefinitionImplementation) datagram.packetDefinition;// getEncoderDecoder().getContentTranslator().getPacketForId(datagram.packetTypeId);
		if (definition.getGenre() == PacketGenre.GENERAL_PURPOSE) {
			Packet packet = definition.createNew(true, null);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getEncoderDecoder());
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.SYSTEM) {
			Packet packet = definition.createNew(true, null);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getEncoderDecoder());
			if (packet instanceof PacketText) {
				handleSystemRequest(((PacketText) packet).text);
			}
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.WORLD) {
			// Queue packets for a specific world
			if (player != null) {
				WorldServer world = (WorldServer) player.getControlledEntity().getWorld();
				world.queueDatagram(datagram, player);
			}
		} else if (definition.getGenre() == PacketGenre.WORLD_STREAMING) {
			// Server doesn't expect world streaming updates from the client
			// it does, however, listen to world_user_requests packets to keep
			// track of the client's world data
			WorldServer world = getEncoderDecoder().getWorld();
			PacketWorldStreaming packet = (PacketWorldStreaming) definition.createNew(false, world);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getEncoderDecoder());
			datagram.dispose();
		} else {
			throw new RuntimeException("whut");
		}
	}

	@Override
	public void pushPacket(Packet packet) {
		try {
			sendQueue.queue(packetsProcessor.buildOutgoingPacket(packet));
		} catch (UnknowPacketException e) {
			logger.error("Couldn't pushPacket()", e);
		} catch (IOException e) {
			close();
		}
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
		return super.close();
	}

	@Override
	public boolean isOpen() {
		return !disconnected;
	}

}
