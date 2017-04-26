package io.xol.chunkstories.api.events.voxel;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.world.VoxelContext;

public abstract class VoxelDestructionEvent extends CancellableEvent
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(VoxelDestructionEvent.class);
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	// Specific event code
	
	final VoxelContext context;

	public VoxelDestructionEvent(VoxelContext context)
	{
		super();
		this.context = context;
	}

	/** Returns the context before the voxel destruction */
	public VoxelContext getContext()
	{
		return context;
	}
}
