//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;

/**
 * Generates a world 'slice' (the voxel cell data represented by a heightmap)
 * using smaller tasks
 */
public class TaskGenerateWorldSlice extends Task implements WorldUser {

	public TaskGenerateWorldSlice(World world, Heightmap heightmap, int directionX, int directionZ) {
		this.world = world;
		this.heightmap = heightmap;

		this.heightmap.registerUser(this);

		this.directionX = directionX;
		this.directionZ = directionZ;
	}

	final World world;
	final Heightmap heightmap;
	int relative_chunkX = 0;
	int directionX;
	int directionZ;
	private Task[] f;

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		// System.out.println(heightmap);
		// if(isWorkDone() || f == null)
		// System.out.println(heightmap.isLoaded()+":"+relative_chunkX+":"+isWorkDone());

		if (!heightmap.isLoaded())
			return false; // wait until that is done

		if (relative_chunkX == 8) {
			if (!isWorkDone()) // not QUITE done yet!
				return false;

			((HeightmapImplementation) this.heightmap).recomputeMetadata();

			// Once the region & it's heightmap has been generated, tell the game client to
			// rebuild them

			//TODO maybe a callback here ?
			//if (world instanceof WorldClient) {
			//	((WorldClient) world).getWorldRenderer().getSummariesTexturesHolder()
			//			.warnDataHasArrived(heightmap.getRegionX(), heightmap.getRegionZ());
			//}

			this.heightmap.save().traverse();
			this.heightmap.unregisterUser(this);
			return true;
		}

		if (f == null || isWorkDone()) {
			int directed_relative_chunkX;
			int directed_relative_chunkZ;

			f = new Task[8];
			for (int relative_chunkZ = 0; relative_chunkZ < 8; relative_chunkZ++) {
				if (directionX < 0) {
					directed_relative_chunkX = 7 - relative_chunkX;
				} else {
					directed_relative_chunkX = relative_chunkX;
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
				f[relative_chunkZ] = task;
			}
			relative_chunkX++;
		}

		return false;
	}

	private boolean isWorkDone() {
		for (int i = 0; i < 8; i++)
			if (!f[i].isDone())
				return false;

		return true;
	}

}
