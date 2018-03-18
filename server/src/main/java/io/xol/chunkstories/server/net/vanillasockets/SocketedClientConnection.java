//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.vanillasockets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.LogicalPacketDatagram;
import io.xol.chunkstories.net.PacketDefinitionImplementation;
import io.xol.chunkstories.net.vanillasockets.SendQueue;
import io.xol.chunkstories.net.vanillasockets.StreamGobbler;
import io.xol.chunkstories.server.net.ClientConnection;
import io.xol.chunkstories.server.net.ClientsManager;
import io.xol.chunkstories.world.WorldServer;

public class SocketedClientConnection extends ClientConnection {

	final ClientsManager clientsManager;
	final Socket socket;
	
	private AtomicBoolean closeOnce = new AtomicBoolean(false);
	private boolean disconnected = false;

	private StreamGobbler streamGobbler;
	private SendQueue sendQueue;
	
	public SocketedClientConnection(ServerInterface server, ClientsManager clientsManager, Socket socket) throws IOException {
		super(server, clientsManager, socket.getInetAddress().getHostAddress(), socket.getPort());
		this.clientsManager = clientsManager;
		this.socket = socket;
		
		//We get exceptions early if this fails
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
	public void handleDatagram(LogicalPacketDatagram datagram) throws IOException, PacketProcessingException, IllegalPacketException {
		PacketDefinitionImplementation definition = (PacketDefinitionImplementation) datagram.packetDefinition;//getPacketsContext().getContentTranslator().getPacketForId(datagram.packetTypeId);
		if (definition.getGenre() == PacketGenre.GENERAL_PURPOSE) {
			Packet packet = definition.createNew(true, null);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getPacketsContext());
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.SYSTEM) {
			Packet packet = definition.createNew(true, null);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getPacketsContext());
			if (packet instanceof PacketText) {
				handleSystemRequest(((PacketText) packet).text);
			}
			datagram.dispose();

		} else if (definition.getGenre() == PacketGenre.WORLD) {
			//Queue packets for a specific world
			if(player != null) {
				WorldServer world = player.getWorld();
				if(world != null) {
					world.queueDatagram(datagram, player);
				}
			}
		} else if (definition.getGenre() == PacketGenre.WORLD_STREAMING) {
			//Server doesn't expect world streaming updates from the client
			//it does, however, listen to world_user_requests packets to keep
			//track of the client's world data
			WorldServer world = getPacketsContext().getWorld();
			PacketWorldStreaming packet = (PacketWorldStreaming) definition.createNew(false, world);
			packet.process(packetsProcessor.getInterlocutor(), datagram.getData(), getPacketsContext());
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
