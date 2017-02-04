package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.item.Inventory;
import io.xol.chunkstories.api.item.InventoryHolder;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityWithInventory extends Entity, InventoryHolder
{	
	public Inventory getInventory();
}
