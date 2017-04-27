package io.xol.chunkstories.api.events.player.voxel;

import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.voxel.VoxelModificationCause;
import io.xol.chunkstories.api.events.voxel.VoxelModificationEvent;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.world.VoxelContext;

public class PlayerVoxelModificationEvent extends VoxelModificationEvent
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(PlayerVoxelModificationEvent.class);
	
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
	final Player player;
	
	public PlayerVoxelModificationEvent(VoxelContext context, int data, VoxelModificationCause cause, Player player)
	{
		super(context, data, cause);
		this.player = player;
	}
	
	public Player getPlayer() {
		return player;
	}
}
