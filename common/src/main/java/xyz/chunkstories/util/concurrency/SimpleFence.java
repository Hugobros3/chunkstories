//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util.concurrency;

import xyz.chunkstories.api.util.concurrency.Fence;

public class SimpleFence implements Fence {
	boolean isOpen = false;

	public void signal() {
		synchronized (this) {
			isOpen = true;
			notifyAll();
		}
	}

	@Override
	public void traverse() {
		while (true) {
			if (isOpen)
				break;

			synchronized (this) {
				if (isOpen)
					break;

				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
