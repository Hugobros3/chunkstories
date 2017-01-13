package io.xol.chunkstories.content.mods.exceptions;

import java.util.Collection;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class NotAllModsLoadedException extends Exception
{
	Collection<ModLoadFailureException> failed;
	
	public NotAllModsLoadedException(Collection<ModLoadFailureException> failed)
	{
		this.failed = failed;
	}
	
	public String getMessage()
	{
		String message = "Some mods failed to load : \n";
		
		for(ModLoadFailureException e : failed)
			message += e.getMessage() + "\n";
		
		return message;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5136184783162902334L;

}
