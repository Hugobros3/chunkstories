package io.xol.chunkstories.api.exceptions.net;

import io.xol.chunkstories.api.net.Packet;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class UnknowPacketException extends Exception
{
	String m;
	
	public UnknowPacketException(int packetType)
	{
		this.m = "Unknown packet ID received : "+packetType;
	}
	
	public UnknowPacketException(Packet packet)
	{
		this.m = "Couldn't determine the ID for the packet : "+packet.getClass().getSimpleName() + ", is it declared in a .packets file ?";
	}
	
	@Override
	public String getMessage()
	{
		return m;
	}
	
	private static final long serialVersionUID = 7612121415158158595L;

}
