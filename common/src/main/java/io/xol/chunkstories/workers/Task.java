package io.xol.chunkstories.workers;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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

	public final boolean run(TaskExecutor taskExecutor)
	{
		if (!done && (cancelled || task(taskExecutor)))
			done = true;
		
		return done;
	}

	protected abstract boolean task(TaskExecutor taskExecutor);
}
