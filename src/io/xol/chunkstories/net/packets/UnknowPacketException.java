package io.xol.chunkstories.net.packets;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class UnknowPacketException extends Exception
{
	int type;
	
	public UnknowPacketException(int packetType)
	{
		this.type = packetType;
	}
	
	public String getMessage()
	{
		return "Unknown packet ID received : "+type;
	}
	
	private static final long serialVersionUID = 7612121415158158595L;

}
