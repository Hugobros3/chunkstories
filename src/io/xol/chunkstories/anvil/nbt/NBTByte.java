package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTByte extends NBTNamed{
	public byte data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		data = (byte)is.read();
	}
}
