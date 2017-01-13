package io.xol.chunkstories.api.net;

import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Some packet types require some step to be applied when added to the send queue
 */
public interface PacketPrepared
{
	/**
	 * Called when the packet is added to the send queue
	 */
	public void prepare(PacketDestinator destinator) throws IOException;
}
