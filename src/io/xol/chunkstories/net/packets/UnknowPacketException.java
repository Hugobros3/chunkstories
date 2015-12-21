package io.xol.chunkstories.net.packets;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class UnknowPacketException extends Exception
{

	
	byte type;
	
	public UnknowPacketException(byte type)
	{
		this.type = type;
	}
	
	public String getMessage()
	{
		return "Unknown packet ID received : "+type;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7612121415158158595L;

}
