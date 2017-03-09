package io.xol.chunkstories.anvil.nbt;

import java.io.IOException;
import java.io.DataInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTInt extends NBTNamed{
	public int data;
	
	@Override
	public void feed(DataInputStream is) throws IOException {
		super.feed(is);
		data = is.read() << 24;
		data += is.read() << 16;
		data += is.read() << 8;
		data += is.read();
	}

	public int getData()
	{
		return data;
	}
}
