//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.iterators;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.CellData;

public class AABBVoxelIterator implements IterableIterator<CellData>, CellData{
	private final World world;
	private final CollisionBox collisionBox;
	
	//private final Voxels voxels;
	
	private int i, j , k;
	private int i2, j2, k2;
	
	private int minx, miny, minz;
	private int maxx, maxy, maxz;
	
	Voxel voxel;
	int sunlight, blocklight, metadata;
	
	public AABBVoxelIterator(World world, CollisionBox collisionBox) {
		this.world = world;
		this.collisionBox = collisionBox;
		
		//this.voxels = world.getGameContext().getContent().voxels();
		
		this.minx = (int)Math.floor(collisionBox.xpos);
		this.miny = (int)Math.floor(collisionBox.ypos);
		this.minz = (int)Math.floor(collisionBox.zpos);
		
		this.maxx = (int)Math.ceil(collisionBox.xpos + collisionBox.xw);
		this.maxy = (int)Math.ceil(collisionBox.ypos + collisionBox.h);
		this.maxz = (int)Math.ceil(collisionBox.zpos + collisionBox.zw);
		
		this.i = minx;
		this.j = miny;
		this.k = minz;
	}
	
	public CollisionBox getCollisionBox() {
		return collisionBox;
	}
	
	@Override
	public boolean hasNext() {
		return k <= maxz;
		/*if(i == maxx && j == maxy && k == maxz)
			return false;
		return true;*/
		//return k <= (int)Math.ceil(collisionBox.zpos + collisionBox.zw);
	}
	@Override
	public CellData next() {
		
		i2 = i;
		j2 = j;
		k2 = k;
		
		i++;
		if(i > maxx) {
			j++;
			i = minx;
		}
		if(j > maxy) {
			k++;
			j = miny;
		}
		if(k > maxz) {
			
		}	//throw new UnsupportedOperationException("Out of bounds iterator. Called when hasNext() returned false.");
		
		//Optimisation here:
		//Instead of making a new CellData object for each iteration we just change this one by pulling the properties
		int raw_data = world.peekRaw(i2, j2, k2);
		voxel = world.getContentTranslator().getVoxelForId(VoxelFormat.id(raw_data));
		sunlight = VoxelFormat.sunlight(raw_data);
		blocklight = VoxelFormat.blocklight(raw_data);
		metadata = VoxelFormat.meta(raw_data);
		
		return this;
	}

	@Override
	public int getX() {
		return i2;
	}

	@Override
	public int getY() {
		return j2;
	}

	@Override
	public int getZ() {
		return k2;
	}
	
	@Override
	public World getWorld() {
		return world;
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
		VoxelSide side = VoxelSide.values()[side_int];
		return world.peekSafely(getX() + side.dx, getY() + side.dy, getZ() + side.dz);
	}
}
