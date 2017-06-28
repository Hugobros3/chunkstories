package io.xol.chunkstories.world.iterators;

import io.xol.chunkstories.api.Content.Voxels;
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
	
	public AABBVoxelIterator(World world, CollisionBox collisionBox) {
		this.world = world;
		this.collisionBox = collisionBox;
		
		this.voxels = world.getGameContext().getContent().voxels();
		
		this.i = (int)Math.floor(collisionBox.xpos);
		this.j = (int)Math.floor(collisionBox.ypos);
		this.k = (int)Math.floor(collisionBox.zpos);
	}
	
	@Override
	public boolean hasNext() {
		return k <= (int)Math.ceil(collisionBox.zpos + collisionBox.zw);
	}
	@Override
	public VoxelContext next() {
		
		i++;
		if(i > (int)Math.ceil(collisionBox.xpos + collisionBox.xw))
			j++;
		if(j > (int)Math.ceil(collisionBox.ypos + collisionBox.h))
			k++;
		if(k > (int)Math.ceil(collisionBox.zpos + collisionBox.zw))
			throw new UnsupportedOperationException("Out of bounds iterator. Called when hasNext() returned false.");
		
		return this;
	}

	@Override
	public Voxel getVoxel() {
		return voxels.getVoxelById(getData());
	}

	@Override
	public int getData() {
		return world.getVoxelData(i, j, k);
	}

	@Override
	public int getX() {
		return i;
	}

	@Override
	public int getY() {
		return j;
	}

	@Override
	public int getZ() {
		return k;
	}

	public int getNeightborData(int side) {
		switch(side) {
		case 0:
			return world.getVoxelData(i - 1, j, k);
		case 1:
			return world.getVoxelData(i, j, k + 1);
		case 2:
			return world.getVoxelData(i + 1, j, k);
		case 3:
			return world.getVoxelData(i, j, k - 1);
		case 4:
			return world.getVoxelData(i, j + 1, k);
		case 5: 
			return world.getVoxelData(i, j - 1, k);
		}
		
		throw new UnsupportedOperationException("getNeighborData(side): Side must be comprised between [0:5]");
	}

	@Override
	public World getWorld() {
		return world;
	}
}
