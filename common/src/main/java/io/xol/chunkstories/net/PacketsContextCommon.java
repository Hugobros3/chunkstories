package io.xol.chunkstories.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.world.WorldNetworked;
import io.xol.chunkstories.net.packets.PacketSendFile;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** The task of the packet processor is to decode & sort incomming packets by ID and to send outcoming packets with the right packet ID. */
public abstract class PacketsContextCommon implements PacketReceptionContext, PacketSendingContext
{
	protected final GameContext gameContext;
	protected final Content.PacketDefinitions store;
	protected final Connection connection;
	
	protected OnlineContentTranslator contentTranslator;

	public PacketsContextCommon(GameContext gameContext, Connection connection) {
		this.gameContext = gameContext;
		this.store = gameContext.getContent().packets();
		this.connection = connection;
	}

	/** Represents whoever we're discussing with */
	public abstract Interlocutor getInterlocutor();
	
	public abstract WorldNetworked getWorld();
	
	public OnlineContentTranslator getContentTranslator() {
		return contentTranslator;
	}

	public void setContentTranslator(OnlineContentTranslator contentTranslator) {
		this.contentTranslator = contentTranslator;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
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
	public LogicalPacketDatagram digestIncommingPacket(DataInputStream in) throws IOException, UnknowPacketException
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
		
		int packetLength = in.readInt();
		byte[] bitme = new byte[packetLength];
		in.read(bitme);
		
		return new PacketIngoingBuffered(packetTypeId, packetLength, bitme);
		
		//PacketDefinitionImpl packetDefinition = (PacketDefinitionImpl) getWorld().getContentTranslator().getPacketForId(packetTypeId);

		/*if(packetDefinition == null) {
			throw new UnknowPacketException(packetTypeId);
		}*/

		/*Packet packet = packetDefinition.createNew(!isServer());

		if (packet == null)
			throw new UnknowPacketException(packetTypeId);
		
		return packet;*/
		
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
	}

	public PacketOutgoingBuffered buildOutgoingPacket(Packet packet) throws UnknowPacketException, IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			short packet_id = findIdForPacket(packet);
			packet.send(getInterlocutor(), dos, this);
			
			PacketOutgoingBuffered buffered = new PacketOutgoingBuffered(this, packet_id, baos.size(), baos.toByteArray());
			return buffered;
		} catch (IOException | UnknowPacketException e) {
			logger().error("Error : unable to buffer Packet " + packet);
			logger().error("{}", e);
			//e.printStackTrace(logger().getPrintWriter());
		}
		return null;
	}

	private short findIdForPacket(Packet packet) throws UnknowPacketException {
		WorldNetworked world = this.getWorld();
		if (world == null) {
			if ((packet instanceof PacketText))
				return 0x00;
			else if ((packet instanceof PacketSendFile))
				return 0x01;
			else // Throw an error if sending a packet while not within a world
				throw new UnknowPacketException(0xFF);
		} else
			return (short) world.getContentTranslator().getIdForPacket(world.getContent().packets().getPacketFromInstance(packet));
	}

	public static final Logger logger = LoggerFactory.getLogger("net.packetsProcessor");
	public Logger logger() {
		return logger;
	}
}
