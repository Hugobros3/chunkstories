package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.EntityLiving;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityFeedable extends EntityLiving {
	public float getFoodLevel();
	
	public void setFoodLevel(float value);
}
