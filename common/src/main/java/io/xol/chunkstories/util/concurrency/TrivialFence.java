//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.concurrency;

import io.xol.chunkstories.api.util.concurrency.Fence;

public class TrivialFence implements Fence {

	@Override
	public void traverse() {
		// Do absolutely nothing
	}

}
