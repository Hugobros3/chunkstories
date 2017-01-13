package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IllegalPacketException extends Exception
{

	Packet packet;
	
	public IllegalPacketException(Packet packet)
	{
		this.packet = packet;
	}

	@Override
	public String getMessage()
	{
		return "Illegal packet received : "+packet.getClass().getName()+"";
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4148448942644331785L;

}
