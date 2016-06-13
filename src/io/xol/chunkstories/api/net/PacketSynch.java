package io.xol.chunkstories.api.net;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Synch packets are read at the begining of a tick and sent at the end
 * Synch packets are meant to be short ; their length is computed when being sent and can be variable
 * Synch packets should never be longer than 256KiB !
 */
public abstract class PacketSynch extends Packet
{
	public PacketSynch(boolean client)
	{
		super(client);
	}

}
