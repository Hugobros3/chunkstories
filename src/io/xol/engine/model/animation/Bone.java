package io.xol.engine.model.animation;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Bone
{
	public String name;
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
	
	Bone parent = null;
	List<Bone> childs = new ArrayList<Bone>();

	public Bone(String name, Bone parent)
	{
		this.name = name;
		this.parent = parent;
	}

	// Pretty recursive debug function :D
	public String toString()
	{
		String txt = "";
		Bone p = parent;
		while (p != null)
		{
			txt = txt + "\t";
			p = p.parent;
		}
		txt += name + " " + channels + " channels, offset=" + offset.toString() + ", dest=" + dest + "\n";
		for (Bone c : childs)
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
		for (Bone c : childs)
			c.recursiveInitMotion(frames);
	}

	// Did I mention how convinient it was ?
	public void recursiveResetFlag()
	{
		animationDataLoaded = false;
		for (Bone c : childs)
			c.recursiveResetFlag();
	}

	/**
	 * Returns a Matrix4f describing how to end up at the bone transformation at the given frame.
	 * @param frame
	 * @return
	 */
	public Matrix4f getTransformationMatrix(int frame)
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
		matrix.rotate(rotX, new Vector3f(1, 0, 0));
		matrix.rotate(rotY, new Vector3f(0, 1, 0));
		matrix.rotate(rotZ, new Vector3f(0, 0, 1));

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
	}

	private float toRad(float f)
	{
		return (float) ((f) / 180 * Math.PI);
	}
}
