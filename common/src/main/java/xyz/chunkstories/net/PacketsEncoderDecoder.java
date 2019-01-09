//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.content.OnlineContentTranslator;
import xyz.chunkstories.api.exceptions.PacketProcessingException;
import xyz.chunkstories.api.exceptions.net.IllegalPacketException;
import xyz.chunkstories.api.exceptions.net.UnknowPacketException;
import xyz.chunkstories.api.net.Interlocutor;
import xyz.chunkstories.api.net.Packet;
import xyz.chunkstories.api.net.PacketDefinition;
import xyz.chunkstories.api.net.PacketReceptionContext;
import xyz.chunkstories.api.net.PacketSendingContext;
import xyz.chunkstories.api.net.packets.PacketText;
import xyz.chunkstories.api.world.WorldNetworked;
import xyz.chunkstories.net.packets.PacketSendFile;

/**
 * The task of the packet processor is to decode & sort incomming packets by ID
 * and to send outcoming packets with the right packet ID.
 */
public abstract class PacketsEncoderDecoder implements PacketReceptionContext, PacketSendingContext {
	protected final Content.PacketDefinitions store;
	protected final Connection connection;

	protected OnlineContentTranslator contentTranslator;

	public PacketsEncoderDecoder(Content.PacketDefinitions packetDefinitions, Connection connection) {
		this.store = packetDefinitions;
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
	 * Read 1 or 2 bytes to getVoxelComponent the next packet ID and returns a packet of this type
	 * if it exists
	 * 
	 * @param in The InputStream of the connection
	 * @return A valid Packet
	 * @throws IOException               If the stream dies when we process it
	 * @throws UnknowPacketException     If the packet id we obtain is invalid
	 * @throws IllegalPacketException    If the packet we obtain is illegal ( if
	 *                                   we're not supposed to receive or send it )
	 * @throws PacketProcessingException
	 */
	public LogicalPacketDatagram digestIncommingPacket(DataInputStream in) throws IOException, UnknowPacketException {
		int firstByte = in.readByte();
		int packetTypeId = 0;
		// If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
		if ((firstByte & 0x80) == 0)
			packetTypeId = firstByte;
		else {
			// It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
			int secondByte = in.readByte();
			secondByte = secondByte & 0xFF;
			packetTypeId = secondByte | (firstByte & 0x7F) << 8;
		}

		PacketDefinitionImplementation def = (PacketDefinitionImplementation) this.getContentTranslator().getPacketForId(packetTypeId);
		if (def == null) {
			throw new UnknowPacketException(packetTypeId);
		}

		if (def.isStreamed() || def.getName().equals("file")) {
			System.out.println("is streamed" + def.isStreamed());
			return new LogicalPacketDatagram(def, -1) {

				@Override
				public DataInputStream getData() {
					return in;
				}

				@Override
				public void dispose() {

				}

			};
		}

		int packetLength = in.readInt();
		byte[] bitme = new byte[packetLength];
		in.readFully(bitme);

		return new PacketIngoingBuffered(def, packetLength, bitme);
	}

	private void writePacketIdHeader(DataOutputStream out, short id) throws IOException {
		if (id < 127)
			out.writeByte((byte) id);
		else {
			out.writeByte((byte) (0x80 | id >> 8));
			out.writeByte((byte) (id % 256));
		}
	}

	public PacketOutgoing buildOutgoingPacket(Packet packet) throws UnknowPacketException, IOException {
		try {
			short packet_id = findIdForPacket(packet);
			if (packet instanceof PacketSendFile) {
				return new PacketOutgoing() {

					@Override
					public void write(DataOutputStream out) throws IOException {
						writePacketIdHeader(out, packet_id);
						packet.send(getInterlocutor(), out, PacketsEncoderDecoder.this);
					}

				};
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			packet.send(getInterlocutor(), dos, this);

			PacketOutgoingBuffered buffered = new PacketOutgoingBuffered(this, packet_id, baos.size(),
					baos.toByteArray());
			return buffered;
		} catch (IOException | UnknowPacketException e) {
			logger().error("Error : unable to buffer Packet " + packet, e);
			// e.printStackTrace(logger().getPrintWriter());
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
		} else {
			PacketDefinition def = world.getContent().packets().getPacketFromInstance(packet);
			if (def == null)
				logger.error("Could not find the definition of packet " + packet);
			short id = (short) world.getContentTranslator().getIdForPacket(def);
			if (id == -1) {
				logger.error("Could not find the id of packet definition " + def.getName());
				// ((AbstractContentTranslator)world.getContentTranslator()).test();
				throw new UnknowPacketException(packet);
			}
			return id;
		}
	}

	public static final Logger logger = LoggerFactory.getLogger("net.packetsProcessor");

	public Logger logger() {
		return logger;
	}
}
