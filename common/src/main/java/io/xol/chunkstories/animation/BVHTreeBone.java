//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.animation;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;

import io.xol.chunkstories.api.animation.SkeletalAnimation.SkeletonBone;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.Quaternion4d;

public class BVHTreeBone implements SkeletonBone
{
	private final BVHAnimation bvh;
	public final String name;
	//final int id;
	
	//Offset from 0.0.0 ( for rigging )
	Vector3f offset;
	
	//Destination vector ( for end bones only )
	Vector3f dest;

	//Float array containing the data, [frames][channel]
	float[][] animationData;
	
	//Hacky thing for loading the bones data
	//TODO hacks will not be tolerated
	boolean animationDataLoaded = false;
	
	//3 is only rotations, 6 is translations included
	int channels = 0;
	
	BVHTreeBone parent = null;
	List<BVHTreeBone> childs = new ArrayList<BVHTreeBone>();

	public BVHTreeBone(String name, BVHTreeBone parent, BVHAnimation bvh)
	{
		this.name = name;
		this.parent = parent;
		this.bvh = bvh;
		//this.id = bvh.bones.size();
	}

	/**
	 * Initializes this bone's and it's children's animationData arrays to empty floats arrays of 
	 */
	public void recursiveInitMotion(int frames)
	{
		animationData = new float[frames][channels];
		for (BVHTreeBone c : childs)
			c.recursiveInitMotion(frames);
	}

	// Did I mention how convinient it was ?
	public void recursiveResetFlag()
	{
		animationDataLoaded = false;
		for (BVHTreeBone c : childs)
			c.recursiveResetFlag();
	}
	
	/**
	 * Returns a Matrix4f describing how to end up at the bone transformation at the given frame.
	 * @param frameLower
	 * @return
	 */
	public Matrix4f getTransformationMatrixInterpolatedRecursive(int frameLower, int frameUpper, double t)
	{
		Matrix4f matrix = getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, t);

		//Apply the father transformation
		if (parent != null) {
			parent.getTransformationMatrixInterpolatedRecursive(frameLower, frameUpper, t).mul(matrix, matrix);
		}
		
		return matrix;
	}

	public Matrix4f getTransformationMatrix(double animationTime)
	{
		double frame = animationTime / 1000.0 / bvh.frameTime;
		
		double frameUpperBound = Math.ceil(frame);
		double frameLowerBound = Math.floor(frame);
		
		double interp = frame % 1.0;
		//Don't try to interpolate if we're on an exact frame
		if(frameLowerBound == frameUpperBound)
			interp = 0.0;
		
		int frameLower = (int)(frameLowerBound) % bvh.frames;
		int frameUpper = (int)(frameUpperBound) % bvh.frames;
		
		return getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, interp);
		//return BVHAnimation.transformBlenderBVHExportToChunkStoriesWorldSpace(getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, interp));
	}
	
	private Matrix4f getTransformationMatrixInterpolatedInternal(int frameLower, int frameUpper, double t)
	{
		//Read rotation data from where it is
		float rotXLower;
		float rotYLower;
		float rotZLower;
		if (channels == 6)
		{
			rotXLower = toRad(animationData[frameLower][3]);
			rotYLower = toRad(animationData[frameLower][4]);
			rotZLower = toRad(animationData[frameLower][5]);
		}
		else
		{
			rotXLower = toRad(animationData[frameLower][0]);
			rotYLower = toRad(animationData[frameLower][1]);
			rotZLower = toRad(animationData[frameLower][2]);
		}
		
		float rotXUpper;
		float rotYUpper;
		float rotZUpper;
		if (channels == 6)
		{
			rotXUpper = toRad(animationData[frameUpper][3]);
			rotYUpper = toRad(animationData[frameUpper][4]);
			rotZUpper = toRad(animationData[frameUpper][5]);
		}
		else
		{
			rotXUpper = toRad(animationData[frameUpper][0]);
			rotYUpper = toRad(animationData[frameUpper][1]);
			rotZUpper = toRad(animationData[frameUpper][2]);
		}

		Quaternion4d quaternionXLower = Quaternion4d.fromAxisAngle(new Vector3d(1.0, 0.0, 0.0), rotXLower);
		Quaternion4d quaternionYLower = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 1.0, 0.0), rotYLower);
		Quaternion4d quaternionZLower = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 0.0, 1.0), rotZLower);
		Quaternion4d totalLower = quaternionXLower.mult(quaternionYLower).mult(quaternionZLower);

		Quaternion4d quaternionXUpper = Quaternion4d.fromAxisAngle(new Vector3d(1.0, 0.0, 0.0), rotXUpper);
		Quaternion4d quaternionYUpper = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 1.0, 0.0), rotYUpper);
		Quaternion4d quaternionZUpper = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 0.0, 1.0), rotZUpper);
		Quaternion4d totalUpper = (quaternionXUpper.mult(quaternionYUpper)).mult(quaternionZUpper);
		
		Quaternion4d total = Quaternion4d.slerp(totalLower, totalUpper, t);
		
		Matrix4f matrix = total.toMatrix4f();
		
		//Apply transformations
		if (channels == 6)
		{
			matrix.m30(matrix.m30() + Math2.mix(animationData[frameLower][0], animationData[frameUpper][0], t));
			matrix.m31(matrix.m31() + Math2.mix(animationData[frameLower][1], animationData[frameUpper][1], t));
			matrix.m32(matrix.m32() + Math2.mix(animationData[frameLower][2], animationData[frameUpper][2], t));
		}
		//TODO check on that, I'm not sure if you should apply both when possible
		else
		{
			matrix.m30(matrix.m30() + offset.x());
			matrix.m31(matrix.m31() + offset.y());
			matrix.m32(matrix.m32() + offset.z());
		}
		
		return BVHAnimation.transformBlenderBVHExportToChunkStoriesWorldSpace(matrix);
	}

	private float toRad(float f)
	{
		return (float) ((f) / 180 * Math.PI);
	}

	@Override
	public BVHTreeBone getParent() {
		return parent;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String toString()
	{
		String txt = "";
		BVHTreeBone p = parent;
		while (p != null)
		{
			txt = txt + "\t";
			p = p.parent;
		}
		txt += name + " " + channels + " channels, offset=" + offset.toString();// + ", dest=" + dest + "\n";
		for (BVHTreeBone c : childs)
			txt += c.toString();

		return "[BVHTreeBone" + txt + "]";
	}

	@Override
	public Matrix4fc getOffsetMatrix() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
