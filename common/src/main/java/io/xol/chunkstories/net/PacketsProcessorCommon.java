package io.xol.chunkstories.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.net.PacketsProcessor;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class PacketsProcessorCommon implements PacketsProcessorActual
{
	private final GameContext gameContext;
	protected final Content.PacketTypes store;
	
	Queue<PendingSynchPacket> pendingSynchPackets = new ConcurrentLinkedQueue<PendingSynchPacket>();

	public PacketsProcessorCommon(GameContext gameContext) {
		this.gameContext = gameContext;
		this.store = gameContext.getContent().packets();
	}
	
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
	public Packet getPacket(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException
	{
		while (true)
		{
			int firstByte = in.readByte();
			int packetType = 0;
			//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
			if ((firstByte & 0x80) == 0)
				packetType = firstByte;
			else
			{
				//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
				int secondByte = in.readByte();
				secondByte = secondByte & 0xFF;
				packetType = secondByte | (firstByte & 0x7F) << 8;
			}
			Packet packet = ((PacketTypeDeclared)store.getPacketTypeById(packetType)).createNew(this instanceof ClientPacketsProcessor);

			//When we get a packetSynch
			if (packet instanceof PacketSynch)
			{
				//Read it's meta
				int packetSynchLength = in.readInt();

				//Read it entirely
				byte[] bufferedIncommingPacket = new byte[packetSynchLength];
				in.readFully(bufferedIncommingPacket);

				//Queue result
				pendingSynchPackets.add(new PendingSynchPacket(packet, bufferedIncommingPacket));
				
				//Skip this packet ( don't return it )
				continue;
			}

			if (packet == null)
				throw new UnknowPacketException(packetType);
			else
				return packet;
		}
		//System.out.println("could not find packut");
		//throw new EOFException();
	}

	/**
	 * Sends the packets header ( ID )
	 * 
	 * @param out
	 * @param packet
	 * @throws UnknowPacketException
	 * @throws IOException
	 */
	public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException
	{
		short id = (short) store.getPacketType(packet).getID();
		if (id < 127)
			out.writeByte((byte) id);
		else
		{
			out.writeByte((byte) (0x80 | id >> 8));
			out.writeByte((byte) (id % 256));
		}
	}
	
	public PendingSynchPacket getPendingSynchPacket()
	{
		return pendingSynchPackets.poll();
	}

	public class PendingSynchPacket
	{
		Packet packet;

		ByteArrayInputStream bais;
		DataInputStream dis;

		public PendingSynchPacket(Packet packet, byte[] buffer)
		{
			this.packet = packet;

			this.bais = new ByteArrayInputStream(buffer);
			this.dis = new DataInputStream(bais);
		}

		public void process(PacketSender sender, PacketsProcessor processor)
		{
			try
			{
				packet.process(sender, dis, processor);
			}
			catch (Exception e)
			{
				/*if (!die) // If the thread was killed then there is no point
							// handling the error.
				{
					// close();
					failed = true;
					latestErrorMessage = "Fatal error while handling connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")";
					System.out.println(latestErrorMessage);
					close();
					e.printStackTrace();
				}*/
				e.printStackTrace();
			}
		}
	}
}
