//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.world.chunk.ChunkLightBaker;

public class TaskGenerateWorldThinSlice extends Task implements WorldUser {

	private final Heightmap heightmap;
	private final World world;
	private final int chunkX, chunkZ;

	private ChunkHolder holders[];

	private final int maxGenerationHeight, maxGenerationHeightInChunks;
	private WorldGenerator generator;

	TaskGenerateWorldThinSlice(World world, int chunkX, int chunkZ, Heightmap heightmap) {
		this.world = world;
		this.heightmap = heightmap;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;

		generator = world.getGenerator();
		maxGenerationHeight = Integer.parseInt(generator.getDefinition().resolveProperty("maxGenerationHeight", "1024"));
		maxGenerationHeightInChunks = (int) Math.ceil(maxGenerationHeight / 32.0);

		holders = new ChunkHolder[maxGenerationHeightInChunks];
		for (int chunkY = 0; chunkY < maxGenerationHeightInChunks; chunkY++) {
			holders[chunkY] = world.acquireChunkHolder(this, chunkX, chunkY, chunkZ);
		}
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		for (int chunkY = 0; chunkY < maxGenerationHeightInChunks; chunkY++) {
			if (!(holders[chunkY].getState() instanceof ChunkHolder.State.Available))
				return false;
		}

		// Doing the lord's work
		Chunk[] chunks = new Chunk[holders.length];
		for (int chunkY = 0; chunkY < maxGenerationHeightInChunks; chunkY++) {
			chunks[chunkY] = holders[chunkY].getChunk();
		}
		generator.generateWorldSlice(chunks);

		// Build the heightmap from that
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				int y = maxGenerationHeight - 1;
				while (y >= 0) {
					CellData cell = holders[y / 32].getChunk().peek(x, y, z);
					if (cell.getVoxel().isSolid() || cell.getVoxel().getName().equals("water")) {
						heightmap.setTopCell(cell);
						break;
					}
					y--;
				}
			}

		// Let there be light
		for (int chunkY = 0; chunkY < maxGenerationHeightInChunks; chunkY++) {
			((ChunkLightBaker) holders[chunkY].getChunk().lightBaker()).hackyUpdateDirect();
		}

		// Let go the world data now
		for (int chunkY = 0; chunkY < maxGenerationHeightInChunks; chunkY++) {
			holders[chunkY].unregisterUser(this);
		}

		return true;
	}
}
