package io.xol.engine.concurrency;

import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Simple blocking lock
 * @author Gobrosse
 *
 */
public class SimpleLock
{
	// Dead-simple lock
	AtomicBoolean locked = new AtomicBoolean();
	
	public synchronized void lock()
	{
		while(locked.get())
			try
			{
				wait(10L);
			}
			catch (InterruptedException e)
			{
				//e.printStackTrace();
				//break;
			}
		locked.set(true);
	}
	
	public synchronized void unlock()
	{
		locked.set(false);
		notify();
	}
}
