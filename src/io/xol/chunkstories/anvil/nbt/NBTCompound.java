package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTCompound extends NBTNamed {
	
	Map<String,NBTNamed> tags = new HashMap<String,NBTNamed>();
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		NBTag tag = NBTag.parse(is);
		while(tag instanceof NBTNamed)
		{
			NBTNamed namedTag = (NBTNamed)tag;
			tags.put(namedTag.name, namedTag);
			tag = NBTag.parse(is);
		}
		//System.out.println("Found TAG_END at "+is.available());
	}
	
	@Override
	public NBTNamed getTag(String path)
	{
		if(path.startsWith("."))
			path = path.substring(1);
		if(path.equals(""))
			return this;
		
		String[] s = path.split("\\.");
		String looking = s[0];
		
		//System.out.println(name+" is asked for "+path+" lf:"+looking);
		
		if(tags.containsKey(looking))
		{
			//System.out.println(name+" contains "+looking);
			return tags.get(looking).getTag(path.replace(looking, ""));
		}
		
		return null;
	}
}
