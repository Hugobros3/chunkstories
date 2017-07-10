package io.xol.chunkstories.api.voxel;

import java.util.Random;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/** Voxel layout version 2.0 */
public class VoxelFormat
{

	//	 This helper class defines the game's saving format
	//	 It stores all voxels in 32-bit signed ( Java won't allow unsigned :c ) ints
	//	 The ints are composed as : 0x0BSMIIII
	//	 0->15 16-bit blockID, allowing for 65536 different blocks types
	//	 16->19 4-bit sunlight
	//	 20->23 4-bit blocklight
	//	 24-31-> 8-bit meta data
	
	public static void main(String a[])
	{
		// Demo-debug
		int data = format(65535, 255, 0, 0);
		data = changeId(data, 15);
		data = changeSunlight(data, 3);
		data = changeBlocklight(data, 7);
		
		System.out.println(data);
		System.out.println("BlockID : " + id(data) + " Meta : " + meta(data) + " Sun : " + sunlight(data) + " Block : " + blocklight(data));

		Random random = new Random();
		int tests = 100000000;
		for(int test = 0; test < tests; test++) {
			int blockId = random.nextInt(65536);
			int blockLight = random.nextInt(16);
			int sunLight = random.nextInt(16);
			int metaData = random.nextInt(256);
			
			int formatted = format(blockId, metaData, sunLight, blockLight);
			if(blockId == id(formatted) && blockLight == blocklight(formatted) && sunLight == sunlight(formatted) && metaData == meta(formatted)) {
				//Ok good
			}
			else {
				System.out.println(formatted);
				System.out.println(blockId + " vs " + id(formatted));
				System.out.println(blockLight + " vs " + blocklight(formatted));
				System.out.println(sunLight + " vs " + sunlight(formatted));
				System.out.println(metaData + " vs " + meta(formatted));
				throw new RuntimeException("Test failed.");
			}

			int blockId2 = random.nextInt(65536);
			int blockLight2 = random.nextInt(16);
			int sunLight2 = random.nextInt(16);
			int metaData2 = random.nextInt(256);
			
			int blockIdExpected = blockId;
			int blockLightExpected = blockLight;
			int sunLightExpected = sunLight;
			int metaDataExpected = metaData;
			
			blockIdExpected = blockId2;
			formatted = changeId(formatted, blockIdExpected);
			
			if(!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted) && metaDataExpected == meta(formatted)))
				throw new RuntimeException("Test failed.");
			
			metaDataExpected = metaData2;
			formatted = changeMeta(formatted, metaDataExpected);
			
			if(!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted) && metaDataExpected == meta(formatted)))
				throw new RuntimeException("Test failed.");
			
			sunLightExpected = sunLight2;
			formatted = changeSunlight(formatted, sunLightExpected);
			
			if(!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted) && metaDataExpected == meta(formatted)))
				throw new RuntimeException("Test failed.");
			
			blockLightExpected = blockLight2;
			formatted = changeBlocklight(formatted, blockLightExpected);
			
			if(!(blockIdExpected == id(formatted) && blockLightExpected == blocklight(formatted) && sunLightExpected == sunlight(formatted) && metaDataExpected == meta(formatted)))
				throw new RuntimeException("Test failed.");
		}
		
		System.out.println("Ran through "+tests+" runs of testing just fine.");
	}

	public static int format(int blockID, int metadata, int sunlight, int blocklight)
	{
		blockID &= 0xFFFF;
		sunlight &= 0xF;
		blocklight &= 0xF;
		metadata &= 0xFF;

		return blockID | metadata << 0x18 | sunlight << 0x10 | blocklight << 0x14;
	}

	public static int id(int src)
	{
		return src & 0xFFFF;
	}

	public static int changeId(int src, int id)
	{
		return src & 0xFFFF0000 | id;
	}

	public static int meta(int src)
	{
		return (src >>> 0x18) & 0xFF;
	}

	public static int changeMeta(int src, int meta)
	{
		return src & 0x00FFFFFF | meta << 0x18;
	}

	public static int sunlight(int src)
	{
		return (src >>> 0x10) & 0xF;
	}

	public static int changeSunlight(int src, int sunlight)
	{
		return src & 0xFFF0FFFF | sunlight << 0x10;
	}

	public static int blocklight(int src)
	{
		return (src >>> 0x14) & 0xF;
	}

	public static int changeBlocklight(int src, int blocklight)
	{
		return src & 0xFF0FFFFF | blocklight << 0x14;
	}
}
