package io.xol.chunkstories.api.ai;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class AI<T extends Entity>
{
	protected T entity;
	protected AiTask currentTask;
	
	public abstract class AiTask {

		public abstract void execute();
	}
	
	public AI(T entity)
	{
		this.entity = entity;
	}
	
	public void tick()
	{
		if(currentTask != null)
			currentTask.execute();
	}
	
	public void setAiTask(AiTask nextTask)
	{
		this.currentTask = nextTask;
	}
}
