package io.xol.chunkstories.anvil.nbt;

import java.io.DataInputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTNamed extends NBTag{

	private String tagName;
	boolean list = false;
	
	@Override
	public void feed(DataInputStream is) throws IOException {
		if(!list)
		{
			int nameSize = 0;
			nameSize += is.read() << 8;
			nameSize += is.read();
			byte[] n = new byte[nameSize];
			try{
				is.readFully(n);
				tagName = new String(n, "UTF-8");
				//System.out.println("read tag named :"+tagName);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String getName()
	{
		return tagName;
	}

	public void setNamedFromListIndex(int i)
	{
		tagName = i+"";
		list = true;
	}

}
