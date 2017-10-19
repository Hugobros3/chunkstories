package io.xol.chunkstories.world.chunk;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.voxel.VoxelsStore;

public class TaskComputeChunkOcclusion extends Task {

	final CubicChunk chunk;
	
	public TaskComputeChunkOcclusion(CubicChunk chunk) {
		this.chunk = chunk;
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		try {
			//Lock this
			chunk.occlusion.onlyOneUpdateAtATime.lock();
			int updatesNeeded = chunk.occlusion.unbakedUpdates.get();
			if(updatesNeeded == 0)
				return true;
			
			chunk.occlusion.occlusionSides = computeOcclusionTable();
			
			//Remove however many updates were pending
			chunk.occlusion.unbakedUpdates.addAndGet(-updatesNeeded);
		}
		finally {
			chunk.occlusion.onlyOneUpdateAtATime.unlock();
		}
		return true;
	}

	static ThreadLocal<Deque<Integer>> occlusionFaces = new ThreadLocal<Deque<Integer>>()
	{
		@Override
		protected Deque<Integer> initialValue()
		{
			return new ArrayDeque<Integer>();
		}
	};
	
	private boolean[][] computeOcclusionTable()
	{
		//System.out.println("Computing occlusion table ...");
		boolean[][] occlusionSides = new boolean[6][6];

		Deque<Integer> deque = occlusionFaces.get();
		deque.clear();
		boolean[] mask = new boolean[32768];
		int x = 0, y = 0, z = 0;
		int completion = 0;
		int p = 0;
		
		@SuppressWarnings("unused")
		int bits = 0;
		//Until all 32768 blocks have been processed
		while (completion < 32768)
		{
			//If this face was already done, we find one that wasn't
			while (mask[x * 1024 + y * 32 + z])
			{
				p++;
				p %= 32768;

				x = p / 1024;
				y = (p / 32) % 32;
				z = p % 32;
			}

			bits++;
			
			//We put this face on the deque
			deque.push(x * 1024 + y * 32 + z);

			/**
			 * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3 BACK z- 4 y+ top X 5 y- bottom
			 */
			Set<Integer> touchingSides = new HashSet<Integer>();
			while (!deque.isEmpty())
			{
				//Pop the topmost element
				int d = deque.pop();

				//Don't iterate twice over one element
				if(mask[d])
					continue;
				
				//Separate coordinates
				x = d / 1024;
				y = (d / 32) % 32;
				z = d % 32;
				
				//Mark the case as done
				mask[x * 1024 + y * 32 + z] = true;
				completion++;
				
				if (!VoxelsStore.get().getVoxelById(chunk.peekSimple(x, y, z)).getType().isOpaque())
				{
					//Adds touched sides to set
					
					if (x == 0)
						touchingSides.add(0);
					else if (x == 31)
						touchingSides.add(2);

					if (y == 0)
						touchingSides.add(5);
					else if (y == 31)
						touchingSides.add(4);

					if (z == 0)
						touchingSides.add(3);
					else if (z == 31)
						touchingSides.add(1);
					
					//Flood fill
					
					if(x > 0)
						deque.push((x - 1) * 1024 + (y) * 32 + (z));
					if(y > 0)
						deque.push((x) * 1024 + (y - 1) * 32 + (z));
					if(z > 0)
						deque.push((x) * 1024 + (y) * 32 + (z - 1));
					
					if(x < 31)
						deque.push((x + 1) * 1024 + (y) * 32 + (z));
					if(y < 31)
						deque.push((x) * 1024 + (y + 1) * 32 + (z));
					if(z < 31)
						deque.push((x) * 1024 + (y) * 32 + (z + 1));
				}
			}
			
			for(int i : touchingSides)
			{
				for(int j : touchingSides)
					occlusionSides[i][j] = true;
			}
		}
		
		return occlusionSides;
	}
	

}
