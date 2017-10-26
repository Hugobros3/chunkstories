package io.xol.chunkstories.voxel.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.IterableIteratorWrapper;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.voxel.components.VoxelComponents;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class VoxelComponentsHolder implements VoxelComponents {

	final CubicChunk chunk;
	final int index;
	
	Map<String, VoxelComponent> map = new HashMap<String, VoxelComponent>();
	
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
	
	public int getIndex() {
		return index;
	}

	@Override
	public void erase() {
		chunk.removeComponents(index);
	}

	@Override
	public void put(String name, VoxelComponent component) {
		map.put(name, component);
	}

	@Override
	public VoxelComponent get(String name) {
		return map.get(name);
	}

	@Override
	public IterableIterator<Entry<String, VoxelComponent>> all() {
		return new IterableIteratorWrapper<Entry<String, VoxelComponent>>(map.entrySet().iterator());
	}

	@Override
	public String name(VoxelComponent component) {
		//Reverse lookup
		Iterator<Entry<String, VoxelComponent>> i = this.map.entrySet().iterator();
		while(i.hasNext()) {
			Entry<String, VoxelComponent> e = i.next();
			if(e.getValue() == component)
				return e.getKey();
		}
		
		return null;
	}

}
