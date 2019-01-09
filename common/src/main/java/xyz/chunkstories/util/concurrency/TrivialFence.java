//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util.concurrency;

import xyz.chunkstories.api.util.concurrency.Fence;

public class TrivialFence implements Fence {

	@Override
	public void traverse() {
		// Do absolutely nothing
	}

}
