package io.xol.chunkstories.item.inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes objects that can be serialized in .csf files (or on the network)
 * @author Gobrosse
 *
 */
public interface CSFSerializable
{
	public void load(DataInputStream stream) throws IOException;

	public void save(DataOutputStream stream) throws IOException;	
}
