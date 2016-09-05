package io.xol.engine.graphics.geometry;

import java.util.Collection;

import io.xol.engine.animation.AnimationData;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderableAnimatable extends Renderable
{
	public void render(RenderingContext renderingContext, AnimationData skeleton, double animationTime);
	
	public void renderInstanciated(RenderingContext renderingContext, Collection<AnimatableData> instancesData);
	
	public class AnimatableData {
		public Vector3f position;
		public AnimationData skeleton;
		public double animationTime;
		public int sunLight, blockLight;
		
		public AnimatableData(Vector3f position, AnimationData skeleton, double animationTime, int sunLight, int blockLight)
		{
			this.position = position;
			this.skeleton = skeleton;
			this.animationTime = animationTime;
			this.sunLight = sunLight;
			this.blockLight = blockLight;
		}
	}
	
	//public void renderParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts);
	
	//public void renderButParts(RenderingContext renderingContext, AnimationData skeleton, double animationTime, String... parts);
}
