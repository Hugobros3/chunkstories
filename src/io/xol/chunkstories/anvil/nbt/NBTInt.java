package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTInt extends NBTNamed{
	public int data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		data = is.read() << 24;
		data += is.read() << 16;
		data += is.read() << 8;
		data += is.read();
	}
}
