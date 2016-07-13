package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.MouseButton;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.EntityChest;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelChest extends VoxelEntity
{
	public VoxelChest(int id, String name)
	{
		super(id, name);
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input, int voxelData)
	{
		//Open GUI
		if(input.equals(MouseButton.RIGHT) && entity.getWorld() instanceof WorldClient)
		{	
			Client.getInstance().openInventory(((EntityChest)this.getVoxelEntity(voxelLocation)).getInventory());
			return true;
		}
		return false;
	}

	@Override
	protected EntityVoxel createVoxelEntity(World world, int x, int y, int z)
	{
		return new EntityChest((WorldImplementation) world, x, y, z);
	}
}
