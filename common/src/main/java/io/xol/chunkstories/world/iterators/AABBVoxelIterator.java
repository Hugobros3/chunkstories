package io.xol.chunkstories.world.iterators;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;

public class AABBVoxelIterator implements IterableIterator<VoxelContext>, VoxelContext{
	private final World world;
	private final CollisionBox collisionBox;
	
	private final Voxels voxels;
	
	private int i, j , k;
	private int i2, j2, k2;
	
	private int minx, miny, minz;
	private int maxx, maxy, maxz;
	
	public AABBVoxelIterator(World world, CollisionBox collisionBox) {
		this.world = world;
		this.collisionBox = collisionBox;
		
		this.voxels = world.getGameContext().getContent().voxels();
		
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
	public VoxelContext next() {
		
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
		
		return this;
	}

	@Override
	public Voxel getVoxel() {
		return voxels.getVoxelById(getData());
	}

	@Override
	public int getData() {
		return world.peekSimple(i2, j2, k2);
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

	public int getNeightborData(int side) {
		switch(side) {
		case 0:
			return world.peekSimple(i2 - 1, j2, k2);
		case 1:
			return world.peekSimple(i2, j2, k2 + 1);
		case 2:
			return world.peekSimple(i2 + 1, j2, k2);
		case 3:
			return world.peekSimple(i2, j2, k2 - 1);
		case 4:
			return world.peekSimple(i2, j2 + 1, k2);
		case 5: 
			return world.peekSimple(i2, j2 - 1, k2);
		}
		
		throw new UnsupportedOperationException("getNeighborData(side): Side must be comprised between [0:5]");
	}

	@Override
	public World getWorld() {
		return world;
	}
}
