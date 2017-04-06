package io.xol.chunkstories.workers;

public abstract class Task
{
	private boolean done = false;
	private boolean cancelled = false;
	
	public boolean isDone()
	{
		return done;
	}
	
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	public void cancel()
	{
		cancelled = true;
	}

	public final boolean run()
	{
		if (!done && (cancelled || task()))
			done = true;
		
		return done;
	}

	abstract boolean task();
}
