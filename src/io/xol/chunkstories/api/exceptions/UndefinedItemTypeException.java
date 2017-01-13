package io.xol.chunkstories.api.exceptions;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class UndefinedItemTypeException extends ItemException
{
	int itemId;
	
	public UndefinedItemTypeException(int itemId)
	{
		this.itemId = itemId;
	}
	
	@Override
	public String getMessage()
	{
		return "Unknown ItemType by id="+itemId;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3629935518207497054L;

}
