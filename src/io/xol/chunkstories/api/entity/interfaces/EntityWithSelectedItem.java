package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.core.entity.components.EntityComponentSelectedItem;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityWithSelectedItem extends EntityWithInventory
{
	public EntityComponentSelectedItem getSelectedItemComponent();
}
