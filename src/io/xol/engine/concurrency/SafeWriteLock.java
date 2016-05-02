package io.xol.engine.concurrency;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SafeWriteLock
{
	AtomicInteger readers = new AtomicInteger();
	AtomicBoolean writing = new AtomicBoolean();

	/**
	 * Returns as soon as reading is safe
	 * If someone is writing it will wait until it's done
	 */
	public synchronized void beginRead()
	{
		while(true)
		{
			synchronized(this)
			{
				if(!writing.get())
				{
					readers.incrementAndGet();
					return;
				}
			}
			System.out.println("writing, wait" + writing.get());
			wait_local();
		}
	}

	/**
	 * Marks the end of a reading process
	 */
	public void endRead()
	{
		readers.decrementAndGet();
		synchronized (this)
		{
			notifyAll();
		}
	}

	/**
	 * Obtains an exclusive lock to write
	 */
	public synchronized void beginWrite()
	{
		//Obtain exclusive writing
		while (!writing.compareAndSet(false, true))
		{
			System.out.println("waiting for exclusive write");
			wait_local();
		}
			//Wait for readers to finish
		while (readers.get() > 0)
		{
			System.out.println("waiting for readers to finish");
			wait_local();
		}
	}

	/**
	 * Releases the write lock
	 */
	public void endWrite()
	{
		writing.set(false);
		synchronized (this)
		{
			notifyAll();
		}
	}

	private synchronized void wait_local()
	{
		try
		{
			wait(10L);
		}
		catch (InterruptedException e)
		{
		}
	}
}
