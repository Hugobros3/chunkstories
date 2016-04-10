package io.xol.chunkstories.item.inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes objects that can be serialized in .csf files (or on the network)
 */
public interface CSFSerializable
{
	/**
	 * Loads the object state from the stream
	 * @param stream
	 * @throws IOException
	 */
	public void loadCSF(DataInputStream stream) throws IOException;

	/**
	 * Writes the object state to a stream
	 * @param stream
	 * @throws IOException
	 */
	public void saveCSF(DataOutputStream stream) throws IOException;	
}
