package io.xol.chunkstories.anvil;

import io.xol.chunkstories.anvil.nbt.NBTByte;
import io.xol.chunkstories.anvil.nbt.NBTByteArray;
import io.xol.chunkstories.anvil.nbt.NBTCompound;
import io.xol.chunkstories.anvil.nbt.NBTag;
import java.io.ByteArrayInputStream;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class MinecraftChunk
{
	NBTCompound root = null;
	int sectionsMap[] = new int[16];

	int cx;
	int cz;

	byte[][] blocks = new byte[16][];
	byte[][] mData = new byte[16][];

	public MinecraftChunk(int x, int z)
	{
		this.cx = x;
		this.cz = z;
	}

	public MinecraftChunk(int x, int z, byte[] byteArray)
	{
		this(x, z);
		ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
		root = (NBTCompound) NBTag.parseInputStream(bais);

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
	
	public NBTCompound getRootTag() {
		return root;
	}

	//TODO This has nothing to do in the lib !
	/*
	public void postProcess(WorldImplementation exported, int csBaseX, int csBaseY, int csBaseZ)
	{
		if(root == null)
			return;
		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");
		if (entitiesList != null)
		{
			//System.out.println("TileEntities:");
			for (NBTag elements : entitiesList.elements)
			{
				//System.out.println("Found TileEntity");
				NBTCompound entity = (NBTCompound) elements;
				NBTString entityId = (NBTString) entity.getTag("id");
				
				int tileX = ((NBTInt)entity.getTag("x")).data;
				int tileY = ((NBTInt)entity.getTag("y")).data;
				int tileZ = ((NBTInt)entity.getTag("z")).data;
				
				int csCoordinatesX = (tileX % 16 + 16) % 16 + csBaseX;
				int csCoordinatesY = tileY;
				int csCoordinatesZ = (tileZ % 16 + 16) % 16 + csBaseZ;
				
				if (entityId.data.equals("Chest"))
				{
					//System.out.println("Found "+entityId.data+" at "+tileX+": "+tileY+" "+tileZ);
				}
				else if (entityId.data.equals("Sign"))
				{
					//System.out.println("Found "+entityId.data+" at "+tileX+": "+tileY+" "+tileZ);
					//System.out.println("aka "+" at "+csCoordinatesX+": "+csCoordinatesY+" "+csCoordinatesZ);
					String text1 = parseSignData(((NBTString)entity.getTag("Text1")).data);
					String text2 = parseSignData(((NBTString)entity.getTag("Text2")).data);
					String text3 = parseSignData(((NBTString)entity.getTag("Text3")).data);
					String text4 = parseSignData(((NBTString)entity.getTag("Text4")).data);
					
					String textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4;
					
					Voxel voxel = VoxelsStore.get().getVoxelById(exported.getVoxelData(csCoordinatesX, csCoordinatesY, csCoordinatesZ));
					//System.out.println("int dataAt sign : "+exported.getVoxelData(csCoordinatesX, csCoordinatesY, csCoordinatesZ));
					if(voxel instanceof VoxelSign)
					{
						//System.out.println("Found a voxel sign matching, setting it's data");
						((EntitySign) ((VoxelSign) voxel).getVoxelEntity(exported, csCoordinatesX, csCoordinatesY, csCoordinatesZ)).setText(textComplete);
						//System.out.println(textComplete);
					}
				}
				//else
				//	System.out.println("Found "+entityId.data);
			}
		}
	}*/

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
					return blocks[i][index] & 0xFF;
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
				return ((NBTByteArray) blocksNBT).data[index] & 0xFF;
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
