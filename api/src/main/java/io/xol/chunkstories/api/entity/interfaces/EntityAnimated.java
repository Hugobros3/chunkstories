package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityAnimated extends Entity
{
	public SkeletonAnimator getAnimatedSkeleton();
}
