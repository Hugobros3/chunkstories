package io.xol.chunkstories.api.events.voxel;

import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.VoxelContext;

public class VoxelModificationEvent extends VoxelEvent
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(VoxelModificationEvent.class);
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}

	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code

	final VoxelModificationCause modificationCause;
	int newData;
	
	public VoxelModificationEvent(VoxelContext context, int data, VoxelModificationCause cause)
	{
		super(context);
		this.newData = data;
		this.modificationCause = cause;
	}

	public int getNewData()
	{
		return newData;
	}

	public void setNewData(int newData)
	{
		this.newData = newData;
	}

	public VoxelModificationCause getModificationCause()
	{
		return modificationCause;
	}

	public Modifiation getModification() {
		if(VoxelFormat.id(getVoxel().getData()) == 0)
			return Modifiation.PLACEMENT;
		else
		{
			if(VoxelFormat.id(newData) == 0)
				return Modifiation.DESTRUCTION;
			else
				return Modifiation.REPLACEMENT;
		}
	}
	
	public enum Modifiation {
		DESTRUCTION,
		PLACEMENT,
		REPLACEMENT;
	}
}
