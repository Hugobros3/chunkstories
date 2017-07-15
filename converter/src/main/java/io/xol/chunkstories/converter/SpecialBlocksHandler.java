package io.xol.chunkstories.converter;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.SignParseUtil;
import io.xol.chunkstories.anvil.nbt.NBTCompound;
import io.xol.chunkstories.anvil.nbt.NBTList;
import io.xol.chunkstories.anvil.nbt.NBTString;
import io.xol.chunkstories.anvil.nbt.NBTInt;
import io.xol.chunkstories.anvil.nbt.NBTag;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.core.entity.voxel.EntitySign;
import io.xol.chunkstories.core.voxel.VoxelSign;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SpecialBlocksHandler {

	public static void processAdditionalStuff(MinecraftChunk minecraftChunk, WorldImplementation exported,
			int csBaseX, int csBaseY, int csBaseZ) {
		
		NBTCompound root = minecraftChunk.getRootTag();
		
		if(root == null)
			return;
		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");
		if (entitiesList != null)
		{
			//System.out.println("TileEntities:");
			for (NBTag element : entitiesList.elements)
			{
				//System.out.println("Found TileEntity" + element);
				NBTCompound entity = (NBTCompound) element;
				NBTString entityId = (NBTString) entity.getTag("id");
				
				int tileX = ((NBTInt)entity.getTag("x")).data;
				int tileY = ((NBTInt)entity.getTag("y")).data;
				int tileZ = ((NBTInt)entity.getTag("z")).data;
				
				int csCoordinatesX = (tileX % 16 + 16) % 16 + csBaseX;
				int csCoordinatesY = tileY;
				int csCoordinatesZ = (tileZ % 16 + 16) % 16 + csBaseZ;
				
				//System.out.println(entityId.data);
				if (entityId.data.toLowerCase().equals("chest"))
				{
					//System.out.println("Found "+entityId.data+" at "+tileX+": "+tileY+" "+tileZ);
				}
				else if (entityId.data.toLowerCase().equals("sign") || entityId.data.toLowerCase().equals("minecraft:sign"))
				{
					//System.out.println("Found "+entityId.data+" at "+tileX+": "+tileY+" "+tileZ);
					//System.out.println("aka "+" at "+csCoordinatesX+": "+csCoordinatesY+" "+csCoordinatesZ);
					String text1 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text1")).data);
					String text2 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text2")).data);
					String text3 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text3")).data);
					String text4 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text4")).data);
					
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
	}

}
