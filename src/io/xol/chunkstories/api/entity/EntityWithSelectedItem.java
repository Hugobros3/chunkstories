package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.entity.core.components.EntityComponentSelectedItem;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityWithSelectedItem extends EntityWithInventory
{
	public EntityComponentSelectedItem getSelectedItemComponent();
}
