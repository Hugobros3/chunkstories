package io.xol.chunkstories.workers;

public abstract class Task
{
	boolean done = false;
	boolean cancelled = false;
	
	public boolean isDone()
	{
		return done;
	}
	
	public void cancel()
	{
		cancelled = true;
	}

	public final boolean run()
	{
		if (!done && (cancelled || runTask()))
			done = true;
		
		return done;
	}

	abstract boolean runTask();
}
