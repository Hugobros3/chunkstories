package io.xol.engine.animation;

import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * For this system to work two assertions need to be valid : all animations used have the same tree structure, and the distances/position of the bones are the same. Otherwrise unpredictable results may occur;
 */
public abstract class AnimatedSkeleton implements SkeletonAnimator
{
	/**
	 * Key of this class : returns whatever BVHAnimation to use with wich bone at wich point, possibly depending of external factors of the implemting subclass
	 */
	public abstract BVHAnimation getAnimationPlayingForBone(String boneName, double animationTime);

	/**
	 * Returns the local matrix to use for a bone, by default grabs it for the playing animation, but you can change that for some effects
	 */
	public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime)
	{
		return getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
	}

	private final Matrix4f getBoneHierarchyTransformationMatrixInternal(String boneName, double animationTime)
	{
		BVHAnimation animation = getAnimationPlayingForBone(boneName, animationTime);
		BVHTreeBone bone = animation.getBone(boneName);
		//Out if null
		if (bone == null)
		{
			System.out.println("null bone");
			return new Matrix4f();
		}

		//Get this very bone transformation matrix
		Matrix4f thisBoneMatrix = getBoneTransformationMatrix(boneName, animationTime);

		//Transform by parent if existant
		BVHTreeBone parent = animation.getBone(boneName).parent;
		if (parent != null)
			Matrix4f.mul(getBoneHierarchyTransformationMatrixInternal(parent.name, animationTime), thisBoneMatrix, thisBoneMatrix);

		return thisBoneMatrix;
	}

	/**
	 * Returns a matrix to offset the rigged mesh
	 */
	public Matrix4f getBoneHierarchyMeshOffsetMatrix(String boneName, double animationTime)
	{
		BVHAnimation animation = getAnimationPlayingForBone(boneName, animationTime);
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
