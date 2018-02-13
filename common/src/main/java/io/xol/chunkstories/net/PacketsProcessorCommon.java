package io.xol.chunkstories.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.packets.PacketDummy;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** The task of the packet processor is to decode & sort incomming packets by ID and to send outcoming packets with the right packet ID. */
public abstract class PacketsProcessorCommon implements PacketReceptionContext, PacketSendingContext
{
	@SuppressWarnings("unused")
	protected final GameContext gameContext;
	protected final Content.PacketTypes store;
	
	public PacketsProcessorCommon(GameContext gameContext) {
		this.gameContext = gameContext;
		this.store = gameContext.getContent().packets();
	}
	
	public abstract PacketSender getSender();
	public abstract PacketDestinator getDestinator();

	public abstract World getWorld();
	
	/**
	 * Read 1 or 2 bytes to get the next packet ID and returns a packet of this type if it exists
	 * 
	 * @param in The InputStream of the connection
	 * @return A valid Packet
	 * @throws IOException If the stream dies when we process it
	 * @throws UnknowPacketException If the packet id we obtain is invalid
	 * @throws IllegalPacketException If the packet we obtain is illegal ( if we're not supposed to receive or send it )
	 * @throws PacketProcessingException 
	 */
	public Packet handleIncommingPacket(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException, PacketProcessingException
	{
		while (true)
		{
			int firstByte = in.readByte();
			int packetTypeId = 0;
			//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
			if ((firstByte & 0x80) == 0)
				packetTypeId = firstByte;
			else
			{
				//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
				int secondByte = in.readByte();
				secondByte = secondByte & 0xFF;
				packetTypeId = secondByte | (firstByte & 0x7F) << 8;
			}
			
			PacketDefinitionImpl packetDefinition = (PacketDefinitionImpl) getWorld().getContentTranslator().getPacketForId(packetTypeId);

			if(packetDefinition == null) {
				throw new UnknowPacketException(packetTypeId);
			}

			Packet packet = packetDefinition.createNew(!isServer());

			if (packet == null)
				throw new UnknowPacketException(packetTypeId);
			
			return packet;
			
			/*switch(packetDefinition.getType()) {
				case GENERAL_PURPOSE:
					packet.process(this.getSender(), in, this);
					break;
				case SYSTEM:
					packet.process(this.getSender(), in, this);
					break;
				case WORLD:
					//TODO: READ COMPLETE PACKET AND FORWARD IT
					break;
				case WORLD_STREAMING:
					//TODO: READ COMPLETE PACKET AND FORWARD IT
					break;
			}*/

			//When we get a packetSynch
			/*if (packet instanceof PacketSynch)
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
			}*/

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
		World world = this.getWorld();
		//Throw an error if sending a packet while not within a world
		if(world == null) {
			if((packet instanceof PacketText))
				out.writeByte(0x00);
			else
				throw new UnknowPacketException(0xFF);
		} else {
			// PacketId header
			short id = (short) world.getContentTranslator().getIdForPacket(world.getContent().packets().getPacketType(packet));
			if (id < 127)
				out.writeByte((byte) id);
			else
			{
				out.writeByte((byte) (0x80 | id >> 8));
				out.writeByte((byte) (id % 256));
			}
			
			// PacketWorld and PacketWorldStreaming headers
			if(packet instanceof PacketWorld) {
				out.writeByte(0x00); // <- worldID. For now no Multiworld support so worldID is always == 0
			} else if(packet instanceof PacketWorldStreaming) {
				out.writeByte(0x00); // <- worldID. For now no Multiworld support so worldID is always == 0
			}
		}
	}
	
	public void sendPacket(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException
	{
		//Dummy packets just get written on the wire immediately
		if(packet instanceof PacketDummy) {
			packet.send(getDestinator(), out, this);
			return;
		}
		
		sendPacketHeader(out, packet);
		packet.send(getDestinator(), out, this);
	}
	
	/*public PendingSynchPacket getPendingSynchPacket()
	{
		return pendingSynchPackets.poll();
	}*/

	/*public class PendingSynchPacket
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
				
				e.printStackTrace();
			}
		}
	}*/
	
	public static final Logger logger = LoggerFactory.getLogger("net.packetsProcessor");
	public Logger logger() {
		return logger;
	}
}
