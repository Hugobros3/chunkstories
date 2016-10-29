package io.xol.chunkstories.api.net;

import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class PacketSynchPrepared extends PacketSynch implements PacketPrepared
{	
	public void prepare(PacketDestinator destinator) throws IOException
	{
		this.sendIntoBuffer(destinator, this.getSynchPacketOutputStream());
		this.finalizeSynchPacket();
	}
	
	public abstract void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException;
}
