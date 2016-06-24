package io.xol.chunkstories.api.voxel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelFormat
{

	//	 This helper class defines the game's saving format
	//	 It stores all voxels in 32-bit signed ( Java won't allow unsigned :c ) ints
	//	 The ints are composed as : 0x0BSMIIII
	//	 0->15 16-bit blockID, allowing for 65536 different blocks types
	//	 16->19 4-bit metadata for simple objects
	//	 20->23 4-bit sunlight
	//	 24-28> 4-bit blocklight
	//   The last 4 bits remain unused as of the current version of the specification.
	
	public static void main(String a[])
	{
		// Demo-debug
		int data = format(0, 13, 0, 0);
		data = changeId(data, 15);
		data = changeSunlight(data, 3);
		System.out.println("BlockID : " + id(data) + "Meta : " + meta(data) + "Sun : " + sunlight(data) + "Block : " + blocklight(data));
	}

	public static int format(int blockID, int metadata, int sunlight, int blocklight)
	{
		blockID %= 0x10000;
		metadata %= 0x10;
		sunlight %= 0x10;
		blocklight %= 0x10;

		return blockID | metadata << 0x10 | sunlight << 0x14 | blocklight << 0x18;
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
		return (src >>> 0x10) & 0xF;
	}

	public static int changeMeta(int src, int meta)
	{
		return src & 0xFFF0FFFF | meta << 0x10;
	}

	public static int sunlight(int src)
	{
		return (src >>> 0x14) & 0xF;
	}

	public static int changeSunlight(int src, int sunlight)
	{
		return src & 0xFF0FFFFF | sunlight << 0x14;
	}

	public static int blocklight(int src)
	{
		return (src >>> 0x18) & 0xF;
	}

	public static int changeBlocklight(int src, int blocklight)
	{
		return src & 0xF0FFFFFF | blocklight << 0x18;
	}
}
