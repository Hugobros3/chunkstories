package io.xol.chunkstories.anvil.nbt;

import java.io.IOException;
import java.io.InputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTLong extends NBTNamed{
	public long data = 0;
	
	@Override
	public void feed(InputStream is) throws IOException {
		super.feed(is);
		
		data = is.read() << 56;
		data += is.read() << 48;
		data += is.read() << 40;
		data += is.read() << 32;
		data += is.read() << 24;
		data += is.read() << 16;
		data += is.read() << 8;
		data += is.read();
	}
}
