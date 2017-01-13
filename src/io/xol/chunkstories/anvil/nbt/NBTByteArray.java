package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTByteArray extends NBTNamed {
	
	int size;
	
	public byte[] data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		size = is.read() << 24;
		size += is.read() << 16;
		size += is.read() << 8;
		size += is.read();
		//System.out.println("byte array of "+size+"b");
		data = new byte[size];
		try {
			is.read(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
