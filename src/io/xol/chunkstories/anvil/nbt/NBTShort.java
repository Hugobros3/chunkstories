package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTShort extends NBTNamed{
	public short data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		int i = is.read() << 8;
		i += is.read();
		data = (short)i;
	}
}
