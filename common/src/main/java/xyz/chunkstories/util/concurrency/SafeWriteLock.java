//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util.concurrency;

public class SafeWriteLock {
	// AtomicInteger readers = new AtomicInteger();
	// SimpleLock readersLock = new SimpleLock();
	SimpleLock writersLock = new SimpleLock();

	int readers = 0;
	int writers = 0;
	int writeRequests = 0;

	/**
	 * Returns as soon as reading is safe If someone is writing it will wait until
	 * it's done
	 */
	public void beginRead() {
		while (writers > 0 || writeRequests > 0)
			wait_local();
		readers++;
	}

	/**
	 * Marks the end of a reading process
	 */
	public void endRead() {
		readers--;
		// notifyAll();
	}

	/**
	 * Obtains an exclusive lock to write
	 */
	public synchronized void beginWrite() {
		writeRequests++;

		while (writers > 0 || readers > 0)
			wait_local();

		writeRequests--;
		writers++;
	}

	/**
	 * Releases the write lock
	 */
	public synchronized void endWrite() {
		writers--;

		notifyAll();
	}

	private synchronized void wait_local() {
		try {
			wait(10L);
		} catch (InterruptedException e) {
		}
	}
}
