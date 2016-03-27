package io.xol.chunkstories.api.input;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describe any form of input
 * @author Hugo
 *
 */
public interface Input
{
	/**
	 * Returns the name of the bind
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns true if the key is pressed
	 * @return
	 */
	public boolean isPressed();
}
