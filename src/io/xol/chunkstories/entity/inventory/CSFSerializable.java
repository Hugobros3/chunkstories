package io.xol.chunkstories.entity.inventory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface CSFSerializable
{
	public void load(DataInputStream stream) throws IOException;

	public void save(DataOutputStream stream) throws IOException;	
}
