package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTIntArray extends NBTNamed {
	
	int size;
	
	public int[] data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		size = is.read() << 24;
		size += is.read() << 16;
		size += is.read() << 8;
		size += is.read();
		//System.out.println("byte array of "+size+"b");
		data = new int[size];
		for(int i = 0; i < size; i++)
		{
			int y = is.read() << 24;
			y += is.read() << 16;
			y += is.read() << 8;
			y += is.read();
			data[i] = y;
		}
	}
}
