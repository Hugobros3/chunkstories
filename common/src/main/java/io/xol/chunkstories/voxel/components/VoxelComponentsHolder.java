package io.xol.chunkstories.voxel.components;

import java.util.Map.Entry;

import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.voxel.components.VoxelComponents;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class VoxelComponentsHolder implements VoxelComponents {

	final CubicChunk chunk;
	final int index;
	
	public VoxelComponentsHolder(CubicChunk chunk, int index) {
		this.chunk = chunk;
		this.index = index;
	}

	@Override
	public Chunk getChunk() {
		return chunk;
	}

	@Override
	public int getX() {
		return chunk.getChunkX() * 32 + index / 1024;
	}

	@Override
	public int getY() {
		return chunk.getChunkY() * 32 + (index / 32) % 32;
	}

	@Override
	public int getZ() {
		return chunk.getChunkZ() * 32 + (index % 32);
	}

	@Override
	public void erase() {
		chunk.removeComponents(index);
	}

	@Override
	public void put(String name, VoxelComponent component) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public VoxelComponent get(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IterableIterator<Entry<String, VoxelComponent>> all() {
		// TODO Auto-generated method stub
		return null;
	}

}
