package io.xol.chunkstories.api.input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a key assignated to some action
 * @author Hugo
 *
 */
public interface KeyBind extends Input
{
	/**
	 * Returns the name of the bind
	 */
	@Override
	public String getName();
	
	/**
	 * Returns true if the key is pressed
	 */
	@Override
	public boolean isPressed();
}
