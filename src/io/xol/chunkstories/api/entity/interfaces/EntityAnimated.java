package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.core.entity.components.EntityComponentAnimation;
import io.xol.engine.animation.AnimatedSkeleton;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityAnimated extends Entity
{
	public EntityComponentAnimation getAnimationComponent();
	
	public AnimatedSkeleton getAnimatedSkeleton();
	
	public String getDefaultAnimation();
}
