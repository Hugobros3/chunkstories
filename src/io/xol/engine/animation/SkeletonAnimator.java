package io.xol.engine.animation;

import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface SkeletonAnimator
{
	/**
	 * Used to draw the debug bone armature
	 */
	public Matrix4f getBoneHierarchyTransformationMatrix(String nameOfEndBone, double animationTime);

	/**
	 * Used to draw deformed mesh parts in OpenGL
	 */
	public Matrix4f getBoneHierarchyTransformationMatrixWithOffset(String nameOfEndBone, double animationTime);
	
	/**
	 * Used to hide body parts and/or do multipass rendering
	 */
	public boolean shouldHideBone(RenderingInterface renderingContext, String boneName);
}
