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
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;
import io.xol.chunkstories.world.storage.RegionImplementation;

/**
 * Generates a world 'slice' (the voxel getCell data represented by a heightmap) using smaller tasks
 */
public class TaskGenerateWorldSlice extends Task implements WorldUser {

	public TaskGenerateWorldSlice(WorldImplementation world, Heightmap heightmap, int directionX, int directionZ) {
		this.world = world;
		this.heightmap = heightmap;

		this.directionX = directionX;
		this.directionZ = directionZ;
	}

	private final WorldImplementation world;
	public final Heightmap heightmap;
	private RegionImplementation[] regions;

	private int wave = 0;

	private int directionX;
	private int directionZ;
	private Task[] tasks;

	private boolean initYet = false;

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		if(!initYet) {
			int heightInRegions = world.getWorldInfo().getSize().heightInChunks / 8;
			this.regions = new RegionImplementation[heightInRegions];
			for(int ry = 0; ry < heightInRegions; ry++) {
				regions[ry] = world.acquireRegion(this, heightmap.getRegionX(), ry, heightmap.getRegionZ());
			}
			initYet = true;
		}

		if (!(heightmap.getState() instanceof Heightmap.State.Generating)) {
			throw new RuntimeException("We only generate world slices when the heightmap data is in the 'Generating' state ! (state="+heightmap.getState()+")");
		}

		if (wave == 8) {
			if (!isWorkDone()) // not QUITE done yet!
				return false;

			((HeightmapImplementation) this.heightmap).recomputeMetadata();

			//TODO maybe a callback here ?
			//heightmap.whenDataAvailable ???

			for(RegionImplementation region : regions) {
				region.unregisterUser(this);
				region.eventGeneratingFinishes$common_main();
			}

			return true;
		}

		if (tasks == null || isWorkDone()) {
			int directed_relative_chunkX;
			int directed_relative_chunkZ;

			tasks = new Task[8];
			for (int relative_chunkZ = 0; relative_chunkZ < 8; relative_chunkZ++) {
				if (directionX < 0) {
					directed_relative_chunkX = 7 - wave;
				} else {
					directed_relative_chunkX = wave;
				}

				if (directionZ < 0) {
					directed_relative_chunkZ = 7 - relative_chunkZ;
				} else {
					directed_relative_chunkZ = relative_chunkZ;
				}

				TaskGenerateWorldThinSlice task = new TaskGenerateWorldThinSlice(world,
						heightmap.getRegionX() * 8 + directed_relative_chunkX,
						heightmap.getRegionZ() * 8 + directed_relative_chunkZ, heightmap);
				world.getGameContext().getTasks().scheduleTask(task);
				tasks[relative_chunkZ] = task;
			}
			wave++;
		}

		return false;
	}

	private boolean isWorkDone() {
		for (int i = 0; i < 8; i++) {
			if (tasks[i].getState() == State.CANCELLED)
				throw new RuntimeException("oh boi no");
			if (tasks[i].getState() != State.DONE)
				return false;
		}

		return true;
	}

}
