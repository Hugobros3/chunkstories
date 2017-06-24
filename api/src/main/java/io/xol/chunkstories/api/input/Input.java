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
	
	/** Returns false if null
	 *  Returns true if o is an input and o.getName().equals(getName())
	 *  Returns true if o is a String and o.equals(getName())
	 *  Returns false otherwise.
	 */
	public boolean equals(Object o);
}
