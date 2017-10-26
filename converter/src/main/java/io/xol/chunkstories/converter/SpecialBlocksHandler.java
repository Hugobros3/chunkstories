package io.xol.chunkstories.converter;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.SignParseUtil;
import io.xol.chunkstories.anvil.nbt.NBTCompound;
import io.xol.chunkstories.anvil.nbt.NBTList;
import io.xol.chunkstories.anvil.nbt.NBTString;
import io.xol.chunkstories.anvil.nbt.NBTInt;
import io.xol.chunkstories.anvil.nbt.NBTag;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SpecialBlocksHandler {

	/*public static void processAdditionalStuff(MinecraftChunk minecraftChunk, WorldImplementation exported,
			int csBaseX, int csBaseY, int csBaseZ) {
		
		NBTCompound root = minecraftChunk.getRootTag();
		
		if(root == null)
			return;
		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");
		if (entitiesList != null)
		{
			for (NBTag element : entitiesList.elements)
			{
				NBTCompound entity = (NBTCompound) element;
				NBTString entityId = (NBTString) entity.getTag("id");
				
				int tileX = ((NBTInt)entity.getTag("x")).data;
				int tileY = ((NBTInt)entity.getTag("y")).data;
				int tileZ = ((NBTInt)entity.getTag("z")).data;
				
				int csCoordinatesX = (tileX % 16 + 16) % 16 + csBaseX;
				int csCoordinatesY = tileY;
				int csCoordinatesZ = (tileZ % 16 + 16) % 16 + csBaseZ;
				
				if (entityId.data.toLowerCase().equals("chest"))
				{
					//Actually we don't bother converting the items
				}
				else if (entityId.data.toLowerCase().equals("sign") || entityId.data.toLowerCase().equals("minecraft:sign"))
				{
					String text1 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text1")).data);
					String text2 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text2")).data);
					String text3 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text3")).data);
					String text4 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text4")).data);
					
					// Chunkstories use a single string instead of 4.
					String textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4;
					
					Chunk chunk = exported.getChunkWorldCoordinates(csCoordinatesX, csCoordinatesY, csCoordinatesZ);
					
					assert chunk != null; // Chunk should really be loaded at this point !!!
					
					ChunkVoxelContext context = chunk.peek(csCoordinatesX, csCoordinatesY, csCoordinatesZ);
					
					assert context.getVoxel() instanceof VoxelSign; // We expect this too
					
					//We grab the component that should have been created while placing the block
					VoxelComponent component = context.components().get("signData");
					
					//Check it exists and is of the right kind
					if(component != null && component instanceof VoxelComponentSignText) {
						VoxelComponentSignText signTextComponent = (VoxelComponentSignText)component;
						signTextComponent.setSignText(textComplete);
					}
					else
						assert false; // or die
					
					//((EntitySign) ((VoxelSign) voxel).getEntity(exported.peekSafely(csCoordinatesX, csCoordinatesY, csCoordinatesZ))).setText(textComplete);
					
				}
				//else
				//	System.out.println("Found "+entityId.data);
			}
		}
	}*/

}
