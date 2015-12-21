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
	Vector3f offset;
	Vector3f dest;

	float[][] animationData;
	boolean animationDataLoaded = false;
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
		txt += name + " " + channels + " channels, offset=" + offset.toString()
				+ ", dest=" + dest + "\n";
		for (Bone c : childs)
			txt += c.toString();

		return txt;
	}

	// I fucking love recusivity.
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

	// Even for geometric transformations it is convinient !
	public Matrix4f getTransformationMatrix(int frame, boolean dekaled)
	{
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
		
		Vector3f totalOffset = new Vector3f();
		Bone kekzer = this;
		while (kekzer != null)
		{
			Vector3f.add(totalOffset, kekzer.offset, totalOffset);
			kekzer = kekzer.parent;
		}
		
		matrix.rotate(rotX, new Vector3f(1, 0, 0));
		matrix.rotate(rotY, new Vector3f(0, 1, 0));
		matrix.rotate(rotZ, new Vector3f(0, 0, 1));

		if (channels == 6)
		{
			matrix.m30 += animationData[frame][0];
			matrix.m31 += animationData[frame][1];
			matrix.m32 += animationData[frame][2];
		}
		else
		{
			matrix.m30 += offset.x;
			matrix.m31 += offset.y;
			matrix.m32 += offset.z;
		}
		
		//if (parent != null) Matrix4f.mul(matrix, parent.getTransformationMatrix(frame, dekaled), matrix);
		
		if(parent != null) Matrix4f.mul(parent.getTransformationMatrix(frame, false), matrix, matrix);
		return matrix;
	}

	private float toRad(float f)
	{
		return (float) ((f) / 180 * Math.PI);
	}
}
