package io.xol.chunkstories.workers;

public abstract class Task
{
	TasksPool<? extends Task> executor;
	boolean done = false;
	
	public void setExecutor(TasksPool<? extends Task> executor)
	{
		this.executor = executor;
	}
	
	public void marksDone()
	{
		done = true;
	}
	
	public boolean isDone()
	{
		return done;
	}
	
	public abstract boolean execute();
	
	public void cancel()
	{
		executor.cancelTask(this);
	}
}
