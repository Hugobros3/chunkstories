package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTString extends NBTNamed{
	public String data;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		
		int size = is.read() << 8;
		size+=is.read();
		
		byte[] n = new byte[size];
		try{
			is.read(n);
			data = new String(n, "UTF-8");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
