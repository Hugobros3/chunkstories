package io.xol.chunkstories.anvil.nbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTDouble extends NBTNamed{
	public double data = 0;
	
	@Override
	public void feed(DataInputStream is) throws IOException {
		super.feed(is);
		
		byte[] bytes = new byte[8];
		try {
			is.readFully(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		data = ByteBuffer.wrap(bytes).getDouble();
	}
	
	public double getData()
	{
		return data;
	}
}
