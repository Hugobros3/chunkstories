package io.xol.chunkstories.importer;

import io.xol.chunkstories.api.voxel.VoxelFormat;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IDsConverter {

	public static int mc2cs(int mcId, int meta) {
		//if(meta != 0)
		//	System.out.println("meta:"+meta+"meta4"+meta%4);
		if(mcId < 0)
			mcId = 256 + mcId;
		if(mcId == 0)
			return 0;
		

		else if(mcId == 78)
			return VoxelFormat.format(17, meta, 0, 0);
		else if(mcId == 80)
			return VoxelFormat.format(37, meta, 0, 0);
		else if(mcId == 60)
			return VoxelFormat.format(36, meta, 0, 0);
		
		else if(mcId == 106)
			return VoxelFormat.format(34, meta, 0, 0);
		else if(mcId == 65)
			return VoxelFormat.format(33, meta, 0, 0);
		
		else if(mcId == 1 && (meta == 5 || meta == 6))
			return VoxelFormat.format(28, meta, 0, 0);
		else if(mcId == 4)
			return 27;
		else if(mcId == 50)
			return VoxelFormat.format(35, 0, 0, 0);
		else if(mcId == 98 && meta == 0)
			return VoxelFormat.format(29, 0, 0, 0);
		else if(mcId == 98 && meta == 1)
			return VoxelFormat.format(31, 0, 0, 0);
		else if(mcId == 98 && meta == 2)
			return VoxelFormat.format(30, 0, 0, 0);

		else if(mcId == 101)
			return VoxelFormat.format(25, 0, 0, 0);
		else if(mcId == 102)
			return VoxelFormat.format(32, 0, 0, 0);
		else if(mcId == 85)
			return VoxelFormat.format(26, 0, 0, 0);
		
		//Clay and whool
		else if(mcId == 35)
			return VoxelFormat.format(20, meta, 0, 0);
		else if(mcId == 66)
			return VoxelFormat.format(66, meta, 0, 0);
		else if(mcId == 159)
			return VoxelFormat.format(24, meta, 0, 0);
		else if(mcId == 45)
			return 23;
		//All slabs
		else if(mcId == 44 && meta % 8 == 0)
			return VoxelFormat.format(44, meta >= 8 ? 1 : 0, 0, 0);
		else if(mcId == 44 && meta % 8 == 3)
			return VoxelFormat.format(47, meta >= 8 ? 1 : 0, 0, 0);
		else if(mcId == 44 && meta % 8 == 5)
			return VoxelFormat.format(48, meta >= 8 ? 1 : 0, 0, 0);
		else if(mcId == 44 && meta % 8 == 4)
			return VoxelFormat.format(45, meta >= 8 ? 1 : 0, 0, 0);
		else if(mcId == 126)
			return VoxelFormat.format(46, meta >= 8 ? 1 : 0, 0, 0);
		
		else if(mcId == 43)
			return 43;
		//Stairs
		else if(mcId == 108)
		{
			meta %= 8;
			return VoxelFormat.format(22, meta, 0, 0);
		}
		else if(mcId == 67)
		{
			meta %= 8;
			return VoxelFormat.format(49, meta, 0, 0);
		}
		else if(mcId == 109)
		{
			meta %= 8;
			return VoxelFormat.format(21, meta, 0, 0);
		}
		else if(mcId == 53 || mcId == 134 || mcId == 135 || mcId == 136)
		{
			meta %= 8;
			return VoxelFormat.format(50, meta, 0, 0);
		}
		//Vegetation
		else if(mcId == 18)
		{
			if(meta % 4 == 2)
				return 19;
			if(meta % 4 == 1)
				return 9;
			return 5;
		}
		else if(mcId == 17)
			return 8;
		//flowers
		else if(mcId == 37)
			return 52;
		else if(mcId == 38)
			return 53;
		
		else if(mcId == 30)
			return 51;
		//Iron
		else if(mcId == 42)
			return 54;
		//Glowstone
		else if(mcId == 89 || mcId == 124)
			return 16;

		else if(mcId == 123)
			return 55;
		
		//doors
		else if(mcId == 64 || mcId == 197 || mcId == 194 || mcId == 71 || mcId == 96 || mcId == 107)
			return 0;
		// buttons and plates
		else if(mcId == 69 || mcId == 77 || mcId == 143 || mcId == 70 || mcId == 72)
			return 0;
		
		else if(mcId == 8 || mcId == 9)
			return 128;
		else if(mcId == 5)
			return 14;
		else if(mcId == 2)
			return 2;
		else if(mcId == 3)
			return 3;
		else if(mcId == 31 || mcId == 37 || mcId == 38)
			return 65;
		else if(mcId == 98 || mcId == 4 || mcId == 43)
			return 10;
		else if(mcId == 12)
			return 12;
		else if(mcId == 11)
			return 11;
		else if(mcId == 20)
			return 15;
		else if(mcId == 106 || mcId == 68 || mcId == 63 || mcId == 175 || mcId == 59 || mcId == 55 || mcId == 65 || mcId == 66 || mcId == 171 || mcId == 132 || mcId == 131)
			return 0;
		else if(mcId == 126 || mcId == 125 || mcId == 85)
			return 14;
		return 1;
	}

}
