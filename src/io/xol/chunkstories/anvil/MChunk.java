package io.xol.chunkstories.anvil;

import io.xol.chunkstories.anvil.nbt.NBTByte;
import io.xol.chunkstories.anvil.nbt.NBTByteArray;
import io.xol.chunkstories.anvil.nbt.NBTCompound;
import io.xol.chunkstories.anvil.nbt.NBTag;

import java.io.ByteArrayInputStream;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class MChunk
{

	NBTCompound root = null;
	int sectionsMap[] = new int[16];

	public MChunk(int x, int z)
	{
		this.cx = x;
		this.cz = z;
	}

	public MChunk(int x, int z, byte[] byteArray)
	{
		this(x, z);
		ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
		root = (NBTCompound) NBTag.parse(bais);

		for (int i = 0; i < 16; i++)
			sectionsMap[i] = -1;
		for (int i = 0; i < 16; i++)
		{
			NBTCompound section = (NBTCompound) root.getTag("Level.Sections." + i);
			if (section != null)
			{
				int y = ((NBTByte) section.getTag("Y")).data;
				sectionsMap[y] = i;

				NBTag blocksNBT = root.getTag("Level.Sections." + i + ".Blocks");
				if (blocksNBT != null)
					blocks[i] = ((NBTByteArray) blocksNBT).data;
			}
		}

		for (int i = 0; i < 16; i++)
		{
			NBTag blocksNBT = root.getTag("Level.Sections." + i + ".Blocks");
			if (blocksNBT != null)
				blocks[i] = ((NBTByteArray) blocksNBT).data;
			NBTag mDataNBT = root.getTag("Level.Sections." + i + ".Data");
			if (mDataNBT != null)
			{
				mData[i] = ((NBTByteArray) mDataNBT).data;
			}
		}
	}

	int cx;
	int cz;

	byte[][] blocks = new byte[16][];
	byte[][] mData = new byte[16][];

	public int getBlockID(int x, int y, int z)
	{
		if (root == null)
			return 0;

		int i = sectionsMap[y / 16];
		if (y > 0 && y < 256)
		{
			if (i >= 0)
			{
				if (blocks[i] != null)
				{
					y %= 16;
					int index = y * 16 * 16 + z * 16 + x;
					return blocks[i][index];
				}
			}
		}

		if (i >= 0)
		{
			NBTag blocksNBT = root.getTag("Level.Sections." + i + ".Blocks");
			if (blocksNBT != null)
			{
				y = y % 16;
				int index = y * 16 * 16 + z * 16 + x;
				return ((NBTByteArray) blocksNBT).data[index];
			}
		}

		return 0;
	}

	public int getBlockMeta(int x, int y, int z)
	{
		int i = sectionsMap[y / 16];
		if (y > 0 && y < 256)
		{
			if (i >= 0)
			{
				if (mData[i] != null)
				{
					y %= 16;
					int index = y * 16 * 16 + z * 16 + x;
					byte unfilteredMeta = mData[i][index / 2];
					//4-bit nibbles, classic bullshit from mojang.
					if (index % 2 != 0)
					{
						return (unfilteredMeta >> 4) & 0xF;
					}
					else
					{
						return (unfilteredMeta) & 0xF;
					}
				}
			}
		}
		return 0;
	}
}
