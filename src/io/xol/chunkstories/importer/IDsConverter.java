package io.xol.chunkstories.importer;

import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.core.voxel.VoxelDoor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IDsConverter
{
	/**
	 * Static method converting blocks only based on local information.
	 * @return	-1 means no block should be applied ( not even set air block )
	 * 			-2 means the complex ( aware of the rest of the world ) method should be called
	 * 			anything else is applied to the world
	 */
	public static int getChunkStoriesIdFromMinecraft(int minecraftBlockId, int minecraftMetaData)
	{
		//Air
		if (minecraftBlockId < 0)
			minecraftBlockId = 256 + minecraftBlockId;
		
		if (minecraftBlockId == 0)
			return 0;
		//quartz, clay, netherrack, bedrock
		else if (minecraftBlockId == 7 || minecraftBlockId == 172 || minecraftBlockId == 155 || minecraftBlockId == 156 || minecraftBlockId == 87 || minecraftBlockId == 112 || minecraftBlockId == 113 || minecraftBlockId == 114)

			return 1;
		//snow
		else if (minecraftBlockId == 78)
			return VoxelFormat.format(17, minecraftMetaData, 0, 0);
		//whole snow
		else if (minecraftBlockId == 80)
			return VoxelFormat.format(37, minecraftMetaData, 0, 0);
		//Farmland
		else if (minecraftBlockId == 60)
			return VoxelFormat.format(36, minecraftMetaData, 0, 0);
		//Wheat etc
		else if (minecraftBlockId == 59 || minecraftBlockId == 141 || minecraftBlockId == 142)
			return VoxelFormat.format(56, minecraftMetaData, 0, 0);
		//Vines
		else if (minecraftBlockId == 106)
			return VoxelFormat.format(34, minecraftMetaData, 0, 0);
		//Ladder
		else if (minecraftBlockId == 65)
			return VoxelFormat.format(33, minecraftMetaData, 0, 0);
		//Custom stone
		else if (minecraftBlockId == 1 && (minecraftMetaData == 5 || minecraftMetaData == 6))
			return VoxelFormat.format(28, minecraftMetaData, 0, 0);
		//Basic Stone
		else if(minecraftBlockId == 1)
			return 1;
		//Ores
		else if(minecraftBlockId == 14 || minecraftBlockId == 15 || minecraftBlockId == 16 || minecraftBlockId == 21 || minecraftBlockId == 56 || minecraftBlockId == 73 || minecraftBlockId == 74 || minecraftBlockId == 129)
		{
			return 1;
		}
		//Cobblestone
		else if (minecraftBlockId == 4)
			return 27;
		//Torch
		else if (minecraftBlockId == 50)
			return VoxelFormat.format(35, 0, 0, 0);
		//Snowbricks
		else if (minecraftBlockId == 98 && minecraftMetaData == 0)
			return VoxelFormat.format(29, 0, 0, 0);
		else if (minecraftBlockId == 98 && minecraftMetaData == 1)
			return VoxelFormat.format(31, 0, 0, 0);
		else if (minecraftBlockId == 98 && minecraftMetaData == 2)
			return VoxelFormat.format(30, 0, 0, 0);
		//Glass pane
		else if (minecraftBlockId == 101)
			return VoxelFormat.format(25, 0, 0, 0);
		//Iron pane
		else if (minecraftBlockId == 102)
			return VoxelFormat.format(32, 0, 0, 0);
		//Fence
		else if (minecraftBlockId == 85)
			return VoxelFormat.format(26, 0, 0, 0);
		//Clay and whool
		else if (minecraftBlockId == 35)
			return VoxelFormat.format(20, minecraftMetaData, 0, 0);
		else if (minecraftBlockId == 66)
			return VoxelFormat.format(66, minecraftMetaData, 0, 0);
		else if (minecraftBlockId == 159)
			return VoxelFormat.format(24, minecraftMetaData, 0, 0);
		else if (minecraftBlockId == 45)
			return 23;
		//All slabs
		else if (minecraftBlockId == 44 && minecraftMetaData % 8 == 0)
			return VoxelFormat.format(44, minecraftMetaData >= 8 ? 1 : 0, 0, 0);
		else if (minecraftBlockId == 44 && minecraftMetaData % 8 == 3)
			return VoxelFormat.format(47, minecraftMetaData >= 8 ? 1 : 0, 0, 0);
		else if (minecraftBlockId == 44 && minecraftMetaData % 8 == 5)
			return VoxelFormat.format(48, minecraftMetaData >= 8 ? 1 : 0, 0, 0);
		else if (minecraftBlockId == 44 && minecraftMetaData % 8 == 4)
			return VoxelFormat.format(45, minecraftMetaData >= 8 ? 1 : 0, 0, 0);
		//wood slab
		else if (minecraftBlockId == 126)
			return VoxelFormat.format(46, minecraftMetaData >= 8 ? 1 : 0, 0, 0);
		//Full halfblock
		else if (minecraftBlockId == 43)
			return 43;
		//Brick Stairs
		else if (minecraftBlockId == 108)
		{
			minecraftMetaData %= 8;
			return VoxelFormat.format(22, minecraftMetaData, 0, 0);
		}
		//Cobble stairs
		else if (minecraftBlockId == 67)
		{
			minecraftMetaData %= 8;
			return VoxelFormat.format(49, minecraftMetaData, 0, 0);
		}
		//Stonebrick stairs
		else if (minecraftBlockId == 109)
		{
			minecraftMetaData %= 8;
			return VoxelFormat.format(21, minecraftMetaData, 0, 0);
		}
		//Wodden stairs
		else if (minecraftBlockId == 53 || minecraftBlockId == 134 || minecraftBlockId == 135 || minecraftBlockId == 136)
		{
			minecraftMetaData %= 8;
			return VoxelFormat.format(50, minecraftMetaData, 0, 0);
		}
		//Vegetation
		else if (minecraftBlockId == 18)
		{
			//Leaves types
			if (minecraftMetaData % 4 == 2)
				return 19;
			if (minecraftMetaData % 4 == 1)
				return 9;
			return 5;
		}
		//Chest
		else if (minecraftBlockId == 54)
		{
			return VoxelFormat.format(97, minecraftMetaData, 0, 0);
		}
		//Sign normal
		else if (minecraftBlockId == 63)
			return VoxelFormat.format(95, minecraftMetaData, 0, 0);
		//Sign post
		else if (minecraftBlockId == 68)
		{
			if (minecraftMetaData == 2)
				minecraftMetaData = 8;
			else if (minecraftMetaData == 3)
				minecraftMetaData = 0;
			else if (minecraftMetaData == 4)
				minecraftMetaData = 4;
			else if (minecraftMetaData == 5)
				minecraftMetaData = 12;
			return VoxelFormat.format(96, minecraftMetaData, 0, 0);
		}
		//Doors
		if (minecraftBlockId == 64 || minecraftBlockId == 71 || minecraftBlockId == 193 || minecraftBlockId == 194 || minecraftBlockId == 195 || minecraftBlockId == 196 || minecraftBlockId == 197)
		{
			return -2;
		}
		//Wood log
		else if (minecraftBlockId == 17)
			return 8;
		//flowers
		else if (minecraftBlockId == 37)
			return 52;
		else if (minecraftBlockId == 38)
			return 53;
		//Web
		else if (minecraftBlockId == 30)
			return 51;
		//Iron blocks
		else if (minecraftBlockId == 42)
			return 54;
		//Glowstone
		else if (minecraftBlockId == 89 || minecraftBlockId == 124)
			return 16;
		//Glowstone but turned off
		else if (minecraftBlockId == 123)
			return 55;
		//doors
		else if (minecraftBlockId == 64 || minecraftBlockId == 197 || minecraftBlockId == 194 || minecraftBlockId == 71 || minecraftBlockId == 96 || minecraftBlockId == 107)
			return 0;
		// buttons and plates
		else if (minecraftBlockId == 69 || minecraftBlockId == 77 || minecraftBlockId == 143 || minecraftBlockId == 70 || minecraftBlockId == 72)
			return 0;
		//Water
		else if (minecraftBlockId == 8 || minecraftBlockId == 9)
			return 128;
		//Wood
		else if (minecraftBlockId == 5)
			return 14;
		//Grass
		else if (minecraftBlockId == 2)
			return 2;
		//Dirt
		else if (minecraftBlockId == 3)
			return 3;
		//Tallgrass
		else if (minecraftBlockId == 31 || minecraftBlockId == 37 || minecraftBlockId == 38)
			return 65;
		//Sand
		else if (minecraftBlockId == 12)
			return 12;
		//Gravel
		else if (minecraftBlockId == 13)
			return 11;
		//Glass
		else if (minecraftBlockId == 20)
			return 15;
		//Ice
		else if (minecraftBlockId == 79)
			return 38;
		//Obsolete catch-stuff
		else if (minecraftBlockId == 106 || minecraftBlockId == 68 || minecraftBlockId == 63 || minecraftBlockId == 175 || minecraftBlockId == 59 || minecraftBlockId == 55 || minecraftBlockId == 65 || minecraftBlockId == 66 || minecraftBlockId == 171 || minecraftBlockId == 132 || minecraftBlockId == 131)
			return 0;
		else if (minecraftBlockId == 126 || minecraftBlockId == 125 || minecraftBlockId == 85)
			return 14;
		return 0;
	}

	public static int getChunkStoriesIdFromMinecraftComplex(int minecraftBlockId, int minecraftMetaData, MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion, int x, int y, int z)
	{
		//System.out.println("Non-baked door");
		//Wooden Doors
		if (minecraftBlockId == 64 || minecraftBlockId == 71 || minecraftBlockId == 193 || minecraftBlockId == 194 || minecraftBlockId == 195 || minecraftBlockId == 196 || minecraftBlockId == 197)
		{
			int csId = 70;
			if(minecraftBlockId == 71)
			{
				csId = 72;
			}
			
			int upper = (minecraftMetaData & 0x8) >> 3;
			int open = (minecraftMetaData & 0x4) >> 2;
			if (upper != 1)
			{
				//int upperId = region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion).getBlockID(x, y + 1, z);
				int upperMeta = region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion).getBlockMeta(x, y + 1, z);
				//System.out.println(upperId == 64);
				int hingeSide = upperMeta & 0x01;
				
				int direction = minecraftMetaData & 0x3;
				return VoxelFormat.format(csId, VoxelDoor.computeMeta(open == 1, hingeSide == 1, VoxelSides.getSideMcDoor(direction)), 0, 0);
			}
			else
				return -1;
		}
		return -1;
	}
}
