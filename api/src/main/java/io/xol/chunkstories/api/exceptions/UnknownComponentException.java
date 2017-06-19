package io.xol.chunkstories.api.exceptions;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class UnknownComponentException extends PacketProcessingException
{
	String message;
	
	public UnknownComponentException(int componentId, Class<? extends Entity> entityClass)
	{
		message = "The componentId : "+componentId+" for the entity "+entityClass.getName()+" was not found";
	}

	public UnknownComponentException(String componentName, Class<? extends Entity> entityClass)
	{
		message = "The componentId : "+componentName+" for the entity "+entityClass.getName()+" was not found";
	}

	@Override
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3592430343334562201L;

}
