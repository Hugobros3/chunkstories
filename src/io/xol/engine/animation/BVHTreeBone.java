package io.xol.engine.animation;

import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.Quaternion4d;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;
import io.xol.engine.math.Math2;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BVHTreeBone
{
	BVHAnimation bvh;
	public String name;
	int id;
	
	//Offset from 0.0.0 ( for rigging )
	Vector3fm offset;
	//Destination vector ( for end bones only )
	Vector3fm dest;

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
		this.id = bvh.bones.size();
	}

	// Pretty recursive debug function :D
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
		txt += name + " " + channels + " channels, offset=" + offset.toString() + ", dest=" + dest + "\n";
		for (BVHTreeBone c : childs)
			txt += c.toString();

		return txt;
	}

	/**
	 * Initializes this bone's and it's children's animationData arrays to empty floats arrays of floats 
	 * @param frames
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
	 * @param frame
	 * @return
	 */
	/*public Matrix4f getTransformationMatrix(int frame)
	{
		//Read rotation data from where it is
		float rotX = toRad(animationData[frame][0]);
		float rotY = toRad(animationData[frame][1]);
		float rotZ = toRad(animationData[frame][2]);
		if (channels == 6)
		{
			rotX = toRad(animationData[frame][3]);
			rotY = toRad(animationData[frame][4]);
			rotZ = toRad(animationData[frame][5]);
		}

		Matrix4f matrix = new Matrix4f();

		//Apply rotations
		matrix.rotate(rotX, new Vector3fm(1, 0, 0));
		matrix.rotate(rotY, new Vector3fm(0, 1, 0));
		matrix.rotate(rotZ, new Vector3fm(0, 0, 1));
		
		//Apply transformations
		if (channels == 6)
		{
			matrix.m30 += animationData[frame][0];
			matrix.m31 += animationData[frame][1];
			matrix.m32 += animationData[frame][2];
		}
		//TODO check on that, I'm not sure if you should apply both when possible
		else
		{
			matrix.m30 += offset.x;
			matrix.m31 += offset.y;
			matrix.m32 += offset.z;
		}

		//Apply the father transformation
		if (parent != null)
			Matrix4f.mul(parent.getTransformationMatrix(frame), matrix, matrix);

		return matrix;
	}*/
	
	/**
	 * Returns a Matrix4f describing how to end up at the bone transformation at the given frame.
	 * @param frameLower
	 * @return
	 */
	public Matrix4f getTransformationMatrixInterpolatedRecursive(int frameLower, int frameUpper, double t)
	{
		Matrix4f matrix = getTransformationMatrixInterpolatedInternal(frameLower, frameUpper, t);

		//Apply the father transformation
		if (parent != null)
			Matrix4f.mul(parent.getTransformationMatrixInterpolatedRecursive(frameLower, frameUpper, t), matrix, matrix);
		
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

		Quaternion4d quaternionXLower = Quaternion4d.fromAxisAngle(new Vector3dm(1.0, 0.0, 0.0), rotXLower);
		Quaternion4d quaternionYLower = Quaternion4d.fromAxisAngle(new Vector3dm(0.0, 1.0, 0.0), rotYLower);
		Quaternion4d quaternionZLower = Quaternion4d.fromAxisAngle(new Vector3dm(0.0, 0.0, 1.0), rotZLower);
		Quaternion4d totalLower = quaternionXLower.mult(quaternionYLower).mult(quaternionZLower);

		Quaternion4d quaternionXUpper = Quaternion4d.fromAxisAngle(new Vector3dm(1.0, 0.0, 0.0), rotXUpper);
		Quaternion4d quaternionYUpper = Quaternion4d.fromAxisAngle(new Vector3dm(0.0, 1.0, 0.0), rotYUpper);
		Quaternion4d quaternionZUpper = Quaternion4d.fromAxisAngle(new Vector3dm(0.0, 0.0, 1.0), rotZUpper);
		Quaternion4d totalUpper = (quaternionXUpper.mult(quaternionYUpper)).mult(quaternionZUpper);
		
		Quaternion4d total = Quaternion4d.slerp(totalLower, totalUpper, t);
		
		Matrix4f matrix = total.toMatrix4f();
		
		//Apply transformations
		if (channels == 6)
		{
			matrix.m30 += Math2.mix(animationData[frameLower][0], animationData[frameUpper][0], t);
			matrix.m31 += Math2.mix(animationData[frameLower][1], animationData[frameUpper][1], t);
			matrix.m32 += Math2.mix(animationData[frameLower][2], animationData[frameUpper][2], t);
		}
		//TODO check on that, I'm not sure if you should apply both when possible
		else
		{
			matrix.m30 += offset.getX();
			matrix.m31 += offset.getY();
			matrix.m32 += offset.getZ();
		}
		
		return matrix;
	}

	private float toRad(float f)
	{
		return (float) ((f) / 180 * Math.PI);
	}
}
