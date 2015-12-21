package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class NBTList extends NBTNamed {
	
	int type;
	int number;
	
	public List<NBTNamed> elements = new ArrayList<NBTNamed>();
	
	@Override
	public void feed(ByteArrayInputStream is) {
		super.feed(is);
		type = is.read();
		number = is.read() << 24;
		number = is.read() << 16;
		number = is.read() << 8;
		number = is.read();
		//System.out.println("Found a list of "+number+" tags of type : "+type+" ("+NBTag.Type.values()[type].name()+")");
		if(type > 0)
		{
			for(int i = 0; i < number; i++)
			{
				NBTag tag = NBTag.create(type);
				tag.list(i);
				tag.feed(is);
				elements.add((NBTNamed) tag);
			}
		}
		else
		{
			//System.out.println("Warning : found a NBTList of TAG_END !");
		}
	}
	
	@Override
	public NBTNamed getTag(String path)
	{
		if(path.startsWith("."))
			path = path.substring(1);
		if(path.equals(""))
			return this;
		
		String[] s = path.split("\\.");
		String parent = s[0];
		
		int index = Integer.parseInt(parent);
		
		//System.out.println(name+" is asked for "+path);
		
		if(index < elements.size())
		{
			//System.out.println(path+"giving element"+index+" rmp:"+path.replace(parent, ""));
			return elements.get(index).getTag(path.replace(parent, ""));
		}
		
		return null;
	}
}
