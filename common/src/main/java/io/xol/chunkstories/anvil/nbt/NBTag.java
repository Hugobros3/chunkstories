package io.xol.chunkstories.anvil.nbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Clean code is for suckers anyway
 */
public abstract class NBTag
{
	public abstract void feed(DataInputStream is) throws IOException;

	public void list_(int i)
	{

	}

	public static NBTag parseInputStream(InputStream bais)
	{
		try
		{
			int type = bais.read();
			if(type == -1)
				return null;
			NBTag tag = NBTag.create(type);
			tag.feed(new DataInputStream(bais));
			return tag;
		}
		catch (IOException e)
		{
			return null;
		}
	}
	
	public static NBTag createNamedFromList(int t, int listIndex)
	{
		NBTag tag = create(Type.values()[t]);
		
		if(tag instanceof NBTNamed)
		{
			NBTNamed named = (NBTNamed)tag;
			named.setNamedFromListIndex(listIndex);
			
			return named;
		}
		
		System.out.println("Error: Type "+t+" ("+Type.values()[t].name()+") can't be named.");
		
		return tag;
	}
	
	static Type lastType;
	
	public static NBTag create(int t)
	{
		try {
			NBTag tag = create(Type.values()[t]);
			lastType = Type.values()[t];
			//System.out.println("found"+lastType);
			return tag;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Out of bounds type exception: "+t+" Last valid type was : "+lastType);
			throw new RuntimeException("Well fuck");
		}
	}

	private static NBTag create(Type t)
	{
		switch (t)
		{
		case TAG_END:
			return new NBTEnd();
		case TAG_COMPOUND:
			return new NBTCompound();
		case TAG_BYTE:
			return new NBTByte();
		case TAG_SHORT:
			return new NBTShort();
		case TAG_INT:
			return new NBTInt();
		case TAG_FLOAT:
			return new NBTFloat();
		case TAG_DOUBLE:
			return new NBTDouble();
		case TAG_STRING:
			return new NBTString();
		case TAG_LONG:
			return new NBTLong();
		case TAG_LIST:
			return new NBTList();
		case TAG_BYTE_ARRAY:
			return new NBTByteArray();
		case TAG_INT_ARRAY:
			return new NBTIntArray();
		default:
			System.out.println("Unknow type : " + t.name());
			break;
		}
		return null;
	}

	public enum Type
	{
		TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY;
	}
}
