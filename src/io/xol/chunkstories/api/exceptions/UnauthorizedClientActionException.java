package io.xol.chunkstories.api.exceptions;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class UnauthorizedClientActionException extends RuntimeException
{
	private static final long serialVersionUID = 800139003416109519L;
	
	String functionCalled;
	
	public UnauthorizedClientActionException(String functionCalled)
	{
		this.functionCalled = functionCalled;
	}
	
	public String getMessage()
	{
		return "Illegal master function : "+functionCalled+" got called but the world is not master.";
	}
}
