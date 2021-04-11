//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.block;

/** A set of helper functions to pack/unpack cell data */
public class VoxelFormat {
	// See chunkstories-docs/engine_tour/world_data.md
	public static final int idMask = 0x0000FFFF;
	public static final int sunlightMask = 0x000F0000;
	public static final int blocklightMask = 0x00F00000;
	public static final int metaMask = 0xFF000000;

	public static final int idBitshift = 0x0;
	public static final int sunBitshift = 0x10;
	public static final int blockBitshift = 0x14;
	public static final int metaBitshift = 0x18;

	public static int format(int blockID, int metadata, int sunlight, int blocklight) {
		blockID &= 0xFFFF;
		sunlight &= 0xF;
		blocklight &= 0xF;
		metadata &= 0xFF;

		return blockID | metadata << 0x18 | sunlight << 0x10 | blocklight << 0x14;
	}

	public static int id(int src) {
		return src & 0xFFFF;
	}

	public static int changeId(int src, int id) {
		return src & 0xFFFF0000 | id;
	}

	public static int meta(int src) {
		return (src >>> 0x18) & 0xFF;
	}

	public static int changeMeta(int src, int meta) {
		return src & 0x00FFFFFF | meta << 0x18;
	}

	public static int sunlight(int src) {
		return (src >>> 0x10) & 0xF;
	}

	public static int changeSunlight(int src, int sunlight) {
		return src & 0xFFF0FFFF | sunlight << 0x10;
	}

	public static int blocklight(int src) {
		return (src >>> 0x14) & 0xF;
	}

	public static int changeBlocklight(int src, int blocklight) {
		return src & 0xFF0FFFFF | blocklight << 0x14;
	}
}
