package io.xol.chunkstories.api.input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describe any form of input (keyboard, mouse, controller(planned), virtual(servers or internal))
 */
public interface Input
{
	/**
	 * Returns the name of the bind
	 */
	public String getName();
	
	/**
	 * Returns true if the key is pressed
	 */
	public boolean isPressed();
	
	/** Returns an unique identifier so server and client can communicate their inputs */
	public long getHash();
}
