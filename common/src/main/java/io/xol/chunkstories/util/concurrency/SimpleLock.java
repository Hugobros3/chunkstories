//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple blocking lock
 * 
 * @author Gobrosse
 */
public class SimpleLock {
	private Thread locker = null;

	// Dead-simple lock
	private AtomicInteger locked = new AtomicInteger();

	public synchronized void lock() {
		// If we haven't already locked it on this thread
		if (locker == null || locker.getId() != Thread.currentThread().getId()) {
			// System.out.println("wait (never)");
			// if(locker != null)
			// System.out.println("Already locked by : "+locker.getId());
			while (locked.get() > 0)
				try {
					wait(10L);
				} catch (InterruptedException e) {
					// e.printStackTrace();
					// break;
				}
		}

		locker = Thread.currentThread();
		// System.out.println("Locked by" + locker.getId());
		locked.incrementAndGet(); // .set(true);
	}

	public synchronized void unlock() {
		locked.decrementAndGet();
		notify();
	}
}
