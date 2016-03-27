package io.xol.chunkstories.api.input;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a key assignated to some action
 * @author Hugo
 *
 */
public interface KeyBind
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
