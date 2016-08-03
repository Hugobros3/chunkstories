package io.xol.engine.graphics.geometry;

import io.xol.engine.animation.AnimationData;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderableAnimatable extends Renderable
{
	public void render(RenderingContext renderingContext, AnimationData skeleton, double animationTime);
	
	public void renderParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts);
	
	public void renderButParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts);
}
