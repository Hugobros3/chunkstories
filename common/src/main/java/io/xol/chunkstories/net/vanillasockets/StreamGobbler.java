package io.xol.chunkstories.net.vanillasockets;

import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.LogicalPacketDatagram;

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
			while(connection.isOpen()) {
				LogicalPacketDatagram datagram = connection.getPacketsContext().digestIncommingPacket(in);
				connection.handleDatagram(datagram);
			}
			
		} catch(IOException e) {
			connection.close();
		} catch (UnknowPacketException e) {
			logger.error("Unknown packet", e);
			connection.close();
		} catch (PacketProcessingException e) {
			connection.close();
			logger.error("Error processing packet", e);
			e.printStackTrace();
		} catch (IllegalPacketException e) {
			logger.error("Illegal packet", e);
			e.printStackTrace();
		}
	}
}
