package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTNamed extends NBTag{

	String name;
	boolean list = false;
	
	@Override
	public void feed(ByteArrayInputStream is) {
		if(!list)
		{
			int nameSize = 0;
			nameSize += is.read() << 8;
			nameSize += is.read();
			byte[] n = new byte[nameSize];
			try{
				is.read(n);
				name = new String(n, "UTF-8");
				//System.out.println("read tag named :"+name);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public NBTNamed getTag(String path)
	{
		/*if(path.equals("name"))
		{
			return this;
		}*/
		return this;
	}

	@Override
	public void list(int i)
	{
		name = i+"";
		list = true;
	}

}
