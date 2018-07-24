//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.util.HashSet;
import java.util.Set;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;

public class TaskComputeChunkOcclusion extends Task {

	final CubicChunk chunk;

	public TaskComputeChunkOcclusion(CubicChunk chunk) {
		this.chunk = chunk;
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		try {
			// Lock this
			chunk.occlusion.onlyOneUpdateAtATime.lock();
			int updatesNeeded = chunk.occlusion.unbakedUpdates.get();
			if (updatesNeeded == 0)
				return true;

			chunk.occlusion.occlusionSides = computeOcclusionTable();

			// Remove however many updates were pending
			chunk.occlusion.unbakedUpdates.addAndGet(-updatesNeeded);
		} finally {
			chunk.occlusion.onlyOneUpdateAtATime.unlock();
		}
		return true;
	}

	static ThreadLocal<IntDeque> occlusionFaces = new ThreadLocal<IntDeque>() {
		@Override
		protected IntDeque initialValue() {
			return new IntArrayDeque();
		}
	};

	static ThreadLocal<boolean[]> masks = new ThreadLocal<boolean[]>() {

		@Override
		protected boolean[] initialValue() {
			return new boolean[32768];
		}

	};

	/*
	 * initialize a smaller piece of the array and use the System.arraycopy call to
	 * fill in the rest of the array in an expanding binary fashion
	 */
	public static void boolfill(boolean[] array, boolean value) {
		int len = array.length;

		if (len > 0) {
			array[0] = value;
		}

		for (int i = 1; i < len; i += i) {
			System.arraycopy(array, 0, array, i, ((len - i) < i) ? (len - i) : i);
		}
	}

	private boolean[][] computeOcclusionTable() {
		// System.out.println("Computing occlusion table ...");
		boolean[][] occlusionSides = new boolean[6][6];

		if (true)
			return occlusionSides;

		IntDeque deque = occlusionFaces.get();
		deque.clear();

		boolean mask[] = masks.get();
		boolfill(mask, false);

		// boolean[] mask = new boolean[32768];
		int x = 0, y = 0, z = 0;
		int completion = 0;
		int p = 0;

		@SuppressWarnings("unused")
		int bits = 0;
		// Until all 32768 blocks have been processed
		while (completion < 32768) {
			// If this face was already done, we find one that wasn't
			while (mask[x * 1024 + y * 32 + z]) {
				p++;
				p %= 32768;

				x = p / 1024;
				y = (p / 32) % 32;
				z = p % 32;
			}

			bits++;

			// We put this face on the deque
			deque.addLast(x * 1024 + y * 32 + z);

			/**
			 * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3
			 * BACK z- 4 y+ top X 5 y- bottom
			 */
			Set<Integer> touchingSides = new HashSet<Integer>();
			while (!deque.isEmpty()) {
				// Pop the topmost element
				int d = deque.removeLast();

				// Don't iterate twice over one element
				if (mask[d])
					continue;

				// Separate coordinates
				x = d / 1024;
				y = (d / 32) % 32;
				z = d % 32;

				// Mark the case as done
				mask[x * 1024 + y * 32 + z] = true;
				completion++;

				if (!chunk.peekSimple(x, y, z).getDefinition().isOpaque()) {
					// Adds touched sides to set

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

					// Flood fill

					if (x > 0)
						deque.addLast((x - 1) * 1024 + (y) * 32 + (z));
					if (y > 0)
						deque.addLast((x) * 1024 + (y - 1) * 32 + (z));
					if (z > 0)
						deque.addLast((x) * 1024 + (y) * 32 + (z - 1));

					if (x < 31)
						deque.addLast((x + 1) * 1024 + (y) * 32 + (z));
					if (y < 31)
						deque.addLast((x) * 1024 + (y + 1) * 32 + (z));
					if (z < 31)
						deque.addLast((x) * 1024 + (y) * 32 + (z + 1));
				}
			}

			for (int i : touchingSides) {
				for (int j : touchingSides)
					occlusionSides[i][j] = true;
			}
		}

		return occlusionSides;
	}

}
