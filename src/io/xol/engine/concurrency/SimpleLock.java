package io.xol.engine.concurrency;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SimpleLock
{
	// Dead-simple lock
	boolean locked = false;
	
	/*long maxWait;
	
	public SimpleLock(long maxWait)
	{
		this.maxWait = maxWait;
	}*/
	
	public synchronized void lock()
	{
		while(locked)
			try
			{
				wait(100L);
			}
			catch (InterruptedException e)
			{
				//e.printStackTrace();
				//break;
			}
		locked = true;
	}
	
	public synchronized void unlock()
	{
		locked = false;
		notify();
	}
}
