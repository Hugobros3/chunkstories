//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.iterators;

import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.api.world.chunk.ChunksIterator;
import xyz.chunkstories.world.storage.RegionImplementation;

/**
 * Iterates over a Region
 */
public class RegionIterator implements ChunksIterator {
	RegionImplementation holder;

	Chunk chunk;
	int i = 0;

	public RegionIterator(RegionImplementation holder) {
		this.holder = holder;
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasNext() {
		while (i < 8 * 8 * 8) {
			chunk = holder.getChunk(i / 64, (i / 8) % 8, i % 8);
			// If there is a chunk waiting then we return yes
			if (chunk != null)
				return true;
			// If not keep looking
			i++;
		}
		// End of chunk
		return false;
	}

	@Override
	public Chunk next() {
		// Returns a chunk and increment by one
		do {
			chunk = holder.getChunk(i / 64, (i / 8) % 8, i % 8);
			i++;
		} while (chunk == null && i < 8 * 8 * 8);

		return chunk;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
