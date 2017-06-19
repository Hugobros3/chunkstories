package io.xol.engine.animation;

import io.xol.chunkstories.api.animation.SkeletalAnimation;
import io.xol.chunkstories.api.animation.SkeletalAnimation.SkeletonBone;
import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.math.Matrix4f;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class to build compound animations: use different animations for each bone, depending on arbitrary parameters.
 * 
 * Works using actual SkeletalAnimation (not SkeletonAnimator! ) obtained through a Content.AnimationsLibrary
 * 
 * For this system to work two assertions need to be valid : all animations used have the same tree structure, and the distances/position of the bones are the same.
 * Otherwrise unpredictable results may occur;
 */
public abstract class CompoundAnimationHelper implements SkeletonAnimator
{
	/**
	 * Key of this class : returns whatever SkeletalAnimation to use with wich bone at wich point, possibly depending of external factors of the implemting subclass
	 */
	public abstract SkeletalAnimation getAnimationPlayingForBone(String boneName, double animationTime);

	/**
	 * Returns the local matrix to use for a bone, by default grabs it for the playing animation, but you can change that for some effects
	 */
	public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime)
	{
		return getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
	}

	private final Matrix4f getBoneHierarchyTransformationMatrixInternal(String boneName, double animationTime)
	{
		SkeletalAnimation animation = getAnimationPlayingForBone(boneName, animationTime);
		SkeletonBone bone = animation.getBone(boneName);
		//Out if null
		if (bone == null)
		{
			System.out.println("null bone");
			return new Matrix4f();
		}

		//Get this very bone transformation matrix
		Matrix4f thisBoneMatrix = getBoneTransformationMatrix(boneName, animationTime);

		//Transform by parent if existant
		SkeletonBone parent = animation.getBone(boneName).getParent();
		if (parent != null)
			Matrix4f.mul(getBoneHierarchyTransformationMatrixInternal(parent.getName(), animationTime), thisBoneMatrix, thisBoneMatrix);

		return thisBoneMatrix;
	}

	/**
	 * Returns a matrix to offset the rigged mesh
	 */
	public Matrix4f getBoneHierarchyMeshOffsetMatrix(String boneName, double animationTime)
	{
		SkeletalAnimation animation = getAnimationPlayingForBone(boneName, animationTime);
		return animation.getOffsetMatrix(boneName);
	}
	
	/**
	 * Used to draw debug bone armature
	 */
	public Matrix4f getBoneHierarchyTransformationMatrix(String nameOfEndBone, double animationTime)
	{
		return BVHAnimation.transformBlenderBVHExportToChunkStoriesWorldSpace(getBoneHierarchyTransformationMatrixInternal(nameOfEndBone, animationTime));
	}

	/**
	 * Used to draw mesh parts in OpenGL
	 */
	public Matrix4f getBoneHierarchyTransformationMatrixWithOffset(String nameOfEndBone, double animationTime)
	{
		Matrix4f matrix = getBoneHierarchyTransformationMatrix(nameOfEndBone, animationTime);

		//Apply the offset matrix
		Matrix4f.mul(matrix, getBoneHierarchyMeshOffsetMatrix(nameOfEndBone, animationTime), matrix);

		return matrix;
	}
}
