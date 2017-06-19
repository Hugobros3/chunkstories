package io.xol.chunkstories.api.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Processes received packets */
public interface PacketsProcessor {
	
	public World getWorld();

	public GameContext getContext();

	/**
	 * Sends the packets header ( ID )
	 * 
	 * @param out
	 * @param packet
	 * @throws UnknowPacketException
	 * @throws IOException
	 */
	public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException;
	
	/**
	 * Read 1 or 2 bytes to get the next packet ID and returns a packet of this type if it exists
	 * 
	 * @param in
	 *            The InputStream of the connection
	 * @return A valid Packet
	 * @throws IOException
	 *             If the stream dies when we process it
	 * @throws UnknowPacketException
	 *             If the packet id we obtain is invalid
	 * @throws IllegalPacketException
	 *             If the packet we obtain is illegal ( if we're not supposed to receive or send it )
	 */
	public Packet getPacket(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException;
}
