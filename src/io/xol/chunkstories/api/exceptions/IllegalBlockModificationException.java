package io.xol.chunkstories.api.exceptions;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Used to restrict block operations, is thrown when a forbidden action is being attempted
 */

public class IllegalBlockModificationException extends Exception
{
	String message;
	
	public IllegalBlockModificationException(String message)
	{
		this.message = message;
	}

	private static final long serialVersionUID = -1717494086092644106L;
}
