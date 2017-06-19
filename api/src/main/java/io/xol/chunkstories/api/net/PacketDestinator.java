package io.xol.chunkstories.api.net;

import io.xol.chunkstories.api.serialization.StreamTarget;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Someone we can send packets to
 */
public interface PacketDestinator extends StreamTarget
{
	public void pushPacket(Packet packet);
	
	public void flush();
	
	public void disconnect();
	
	public void disconnect(String disconnectionReason);
}
