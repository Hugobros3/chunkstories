package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.engine.animation.SkeletonAnimator;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityAnimated extends Entity
{
	public SkeletonAnimator getAnimatedSkeleton();
}
