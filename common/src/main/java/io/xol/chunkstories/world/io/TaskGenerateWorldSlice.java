package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.world.summary.HeightmapImplementation;

/** Generates a world 'slice' (the voxel cell data represented by a heightmap) using smaller tasks */
public class TaskGenerateWorldSlice extends Task implements WorldUser {

	public TaskGenerateWorldSlice(World world, Heightmap heightmap) {
		this.world = world;
		this.heightmap = heightmap;
		
		this.heightmap.registerUser(this);
	}
	
	final World world;
	final Heightmap heightmap;
	int relative_chunkX = 0;
	private Task[] f;
	
	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		//System.out.println(heightmap);
		//if(isWorkDone() || f == null)
		//	System.out.println(heightmap.isLoaded()+":"+relative_chunkX+":"+isWorkDone());
		
		if(!heightmap.isLoaded())
			return false; //wait until that is done
		
		if(relative_chunkX == 8) {
			((HeightmapImplementation) this.heightmap).recomputeMetadata();
			this.heightmap.save().traverse();
			this.heightmap.unregisterUser(this);
			return true;
		}
		
		if(f == null || isWorkDone()) {
			f = new Task[8];
			for(int relative_chunkZ = 0; relative_chunkZ < 8; relative_chunkZ++) {
				TaskGenerateWorldThinSlice task = new TaskGenerateWorldThinSlice(world, heightmap.getRegionX() * 8 + relative_chunkX,heightmap.getRegionZ() * 8 + relative_chunkZ, heightmap);
				world.getGameContext().tasks().scheduleTask(task);
				f[relative_chunkZ] = task;
			}
			relative_chunkX++;
		}
		
		return false;
	}

	private boolean isWorkDone() {
		for(int i = 0; i < 8; i++)
			if(!f[i].isDone())
				return false;
		
		return true;
	}

}
