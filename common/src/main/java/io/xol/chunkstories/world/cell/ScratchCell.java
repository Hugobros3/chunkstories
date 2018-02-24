//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.cell;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.CellData;

/** Used to recycle results of a peek command */
public class ScratchCell implements CellData {
	final World world;
	
	public ScratchCell(World world) {
		this.world = world;
	}
	
	// Fields set to public so we can access them
	public int x, y, z;
	public Voxel voxel;
	public int sunlight, blocklight, metadata;
	
	@Override
	public World getWorld() {
		return world;
	}
	@Override
	public int getX() {
		return x;
	}
	@Override
	public int getY() {
		return y;
	}
	@Override
	public int getZ() {
		return z;
	}
	@Override
	public Voxel getVoxel() {
		return voxel;
	}
	@Override
	public int getMetaData() {
		return metadata;
	}
	@Override
	public int getSunlight() {
		return sunlight;
	}
	@Override
	public int getBlocklight() {
		return blocklight;
	}
	@Override
	public CellData getNeightbor(int side_int) {
		VoxelSides side = VoxelSides.values()[side_int];
		return world.peekSafely(x + side.dx, y + side.dy, z + side.dz);
	}
}
