//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net.vanillasockets;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.exceptions.net.UnknowPacketException;
import xyz.chunkstories.net.Connection;
import xyz.chunkstories.net.LogicalPacketDatagram;

/** Eats what a InputStream provides and digests it */
public abstract class StreamGobbler extends Thread {
	private final Connection connection;
	private final DataInputStream in;

	protected static final Logger logger = LoggerFactory.getLogger("client.net.connection.in");

	public StreamGobbler(Connection connection, DataInputStream in) {
		this.connection = connection;
		this.in = in;
	}

	@Override
	public void run() {
		try {
			while (connection.isOpen()) {
				LogicalPacketDatagram datagram = connection.getEncoderDecoder().digestIncommingPacket(in);
				connection.handleDatagram(datagram);
			}

		} catch (SocketException e) { // Natural
			if (connection.isOpen()) {
				logger.info("Closing socket.");
			}
			connection.close();
		} catch (EOFException e) { // Natural too
			connection.close();
			logger.info("Connection closed");
		} catch (IOException e) {
			connection.close();
			logger.info("Connection error", e);
		} catch (UnknowPacketException e) {
			logger.error("Unknown packet", e);
			connection.close();
		} catch (PacketProcessingException e) {
			connection.close();
			logger.error("Error processing packet", e);
		} catch (IllegalPacketException e) {
			logger.error("Illegal packet", e);
			connection.close();
		}
	}
}
