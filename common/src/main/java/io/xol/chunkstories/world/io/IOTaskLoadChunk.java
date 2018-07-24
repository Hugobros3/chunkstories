//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;

public class IOTaskLoadChunk extends IOTask {
	ChunkHolderImplementation chunkSlot;

	public IOTaskLoadChunk(ChunkHolderImplementation chunkSlot) {
		this.chunkSlot = chunkSlot;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		// When a loader was removed from the world, remaining operations on it are
		// discarded
		if (chunkSlot.getRegion().isUnloaded())
			return true;

		// And so are redudant operations
		if (chunkSlot.isChunkLoaded())
			return true;

		/*
		 * Region region = chunkSlot.getRegion(); Region actualRegion =
		 * world.getRegion(region.getRegionX(), region.getRegionY(),
		 * region.getRegionZ());
		 * 
		 * if(region != actualRegion) { System.out.
		 * println("Some quircky race condition led to this region being discarded then loaded again but without raising the isUnloaded() flag !"
		 * ); System.out.println(region + " vs: " + actualRegion); return true; }
		 */

		// If for some reason the chunks holder's are still not loaded, we requeue the
		// job
		if (!chunkSlot.getRegion().isDiskDataLoaded())
			return false;

		CompressedData compressedData = chunkSlot.getCompressedData();
		if (compressedData == null) {
			/* Chunk chunk = */chunkSlot.createChunk();
			// Don't generate the chunks here, actually.
			// world.getGenerator().generateChunk(chunk);
		}
		// Normal voxel data is present, uncompressed it then load it to the chunk
		else {
			/* CubicChunk chunk = */chunkSlot.createChunk(compressedData);
			// Postprocess ?
		}

		return true;
	}

	@Override
	public String toString() {
		return "[IOTaskLoadChunk " + chunkSlot + "]";
	}
}