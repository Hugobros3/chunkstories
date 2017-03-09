package io.xol.chunkstories.anvil.nbt;

import java.io.IOException;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTFloat extends NBTNamed{
	public float data = 0;
	
	@Override
	public void feed(DataInputStream is) throws IOException {
		super.feed(is);
		byte[] bytes = new byte[4];
		try {
			is.readFully(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		data = ByteBuffer.wrap(bytes).getFloat();
	}
}
