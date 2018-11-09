//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.concurrency;

import java.util.LinkedList;

import io.xol.chunkstories.api.util.concurrency.Fence;

public class CompoundFence extends LinkedList<Fence> implements Fence {

	private static final long serialVersionUID = 1770973697744619763L;

	@Override
	/** Traverse-all :) */
	public void traverse() {
		for (Fence f : this) {
			if (f != null)
				f.traverse();
		}
	}

}
