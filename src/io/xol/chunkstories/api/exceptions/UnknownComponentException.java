package io.xol.chunkstories.api.exceptions;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class UnknownComponentException extends PacketProcessingException
{
	int componentId;
	Class<? extends Entity> entityClass;
	
	public UnknownComponentException(int componentId, Class<? extends Entity> entityClass)
	{
		this.componentId = componentId;
		this.entityClass = entityClass;
	}

	@Override
	public String getMessage()
	{
		return "The componentId : "+componentId+" for the entity "+entityClass.getName()+" was not found";
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3592430343334562201L;

}
