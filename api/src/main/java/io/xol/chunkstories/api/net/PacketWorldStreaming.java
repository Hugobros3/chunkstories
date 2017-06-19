package io.xol.chunkstories.api.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.world.WorldMaster;

/** Used for async world streaming */
public abstract class PacketWorldStreaming extends Packet {
	
	final static private byte UNUSED = 0x00;
	final static private byte INVALID = (byte) 0xFF;
	
	/** For future multi-world support, server.worlds[worldId] = world
	Letting the client know of this is relevant because these packets may be sent asynchronously and the client has to be able to reject them if they
	are for a world we are no longer in. */
	private byte worldID = INVALID;
	
	public PacketWorldStreaming()
	{
		
	}
	
	public PacketWorldStreaming(WorldMaster world)
	{
		worldID = UNUSED;
	}

	/** Must be called first by extended classes */
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeByte(0xFF & worldID);
	}

	/** Must be called first by extended classes */
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		worldID = in.readByte();
	}
}
