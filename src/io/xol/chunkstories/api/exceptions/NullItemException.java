package io.xol.chunkstories.api.exceptions;

import java.io.DataInputStream;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class NullItemException extends ItemException
{
	DataInputStream stream;
	
	public NullItemException(DataInputStream stream)
	{
		this.stream = stream;
	}
	
	@Override
	public String getMessage()
	{
		return "(Notice) Read a null ItemPile (ItemId=0) from stream "+stream;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8788184589175791958L;
}
