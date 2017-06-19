package io.xol.chunkstories.api.animation;

import io.xol.chunkstories.api.math.Matrix4f;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Describes a full animation, loaded from Content.AnimationsLibrary */
public interface SkeletalAnimation extends SkeletonAnimator {

	/** How is that bone offset to the center of the model */
	public Matrix4f getOffsetMatrix(String boneName);
	
	public SkeletonBone getBone(String boneName);
	
	public interface SkeletonBone {

		public Matrix4f getTransformationMatrix(double animationTime);
		
		public SkeletonBone getParent();

		public String getName();
	}
}
