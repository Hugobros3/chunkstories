package io.xol.chunkstories.anvil.nbt;

import java.io.IOException;
import java.io.InputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTByte extends NBTNamed{
	public byte data;
	
	@Override
	public void feed(InputStream is) throws IOException {
		super.feed(is);
		data = (byte)is.read();
	}
}
