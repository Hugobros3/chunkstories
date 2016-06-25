package io.xol.chunkstories.anvil.nbt;

import java.io.ByteArrayInputStream;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Clean code is for suckers anyway
 * 
 * @author Gobrosse
 */
public abstract class NBTag
{

	public abstract void feed(ByteArrayInputStream is);

	public void list(int i)
	{

	}

	public static NBTag parse(ByteArrayInputStream bais)
	{
		int type = bais.read();
		NBTag tag = NBTag.create(type);
		tag.feed(bais);
		return tag;
	}

	public static NBTag create(int t)
	{
		return create(Type.values()[t]);
	}

	public static NBTag create(Type t)
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
