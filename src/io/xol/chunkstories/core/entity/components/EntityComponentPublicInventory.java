package io.xol.chunkstories.core.entity.components;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentPublicInventory extends EntityComponentInventory
{
	public EntityComponentPublicInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, width, height);
	}

	@Override
	public void refreshCompleteInventory()
	{
		if(this.entity.getWorld() instanceof WorldMaster)
			this.pushComponentEveryone();
	}
	
	@Override
	public String getHolderName()
	{
		return "Chest";
	}
}
