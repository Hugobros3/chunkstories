package io.xol.engine.model.animation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BVHAnimation
{

	int frames = 0;
	float frameTime = 0f;

	Bone root;
	public List<Bone> bones = new ArrayList<Bone>();

	public static void main(String a[])
	{
		BVHAnimation test = new BVHAnimation(new File("res/models/human.bvh"));
		System.out.println(test.toString());
	}

	public BVHAnimation(File file)
	{
		load(file);
	}

	public Matrix4f getTransformationForBone(String boneName, int frame)
	{
		Matrix4f matrix = new Matrix4f();
		if (frames == 0)
		{
			System.out.println("fack you");
			return matrix;
		}
		Matrix4f rotMatrix = new Matrix4f();
		rotMatrix.m11 = 0;
		rotMatrix.m22 = 0;
		rotMatrix.m12 = 1;
		rotMatrix.m21 = 1;

		frame %= frames;
		for (Bone b : bones)
			if (b.name.equals(boneName))
			{
				Matrix4f.mul(b.getTransformationMatrix(frame, false), matrix, matrix);
			}

		Matrix4f.mul(rotMatrix, matrix, matrix);
		Matrix4f.mul(matrix, rotMatrix, matrix);

		// System.out.println(rotMatrix.toString());
		return matrix;
	}

	public Matrix4f getTransformationForBonePlusOffset(String boneName, int frame)
	{
		Matrix4f matrix = new Matrix4f();
		if (frames == 0)
		{
			System.out.println("fack you");
			return matrix;
		}
		Matrix4f offsetMatrix = new Matrix4f();
		Matrix4f rotMatrix = new Matrix4f();

		rotMatrix.m11 = 0;
		rotMatrix.m22 = 0;
		rotMatrix.m12 = 1;
		rotMatrix.m21 = 1;

		Vector3f offsetTotal = new Vector3f();

		frame %= frames;
		for (Bone b : bones)
			if (b.name.equals(boneName))
			{
				Bone kek = b;
				while (kek != null)
				{
					offsetTotal.x += kek.offset.x;
					offsetTotal.y += kek.offset.z;
					offsetTotal.z += kek.offset.y;
					kek = kek.parent;
				}
				offsetTotal.negate();

				offsetMatrix.m30 += offsetTotal.x;
				offsetMatrix.m31 += offsetTotal.y;
				offsetMatrix.m32 += offsetTotal.z;
			}
		
		matrix = getTransformationForBone(boneName, frame);
		
		// Matrix4f.mul(matrix, offsetMatrix, matrix);

		// matrix = new Matrix4f();

		// offsetMatrix.rotate((float)Math.PI/2, new Vector3f(0, 0, 1));

		// Matrix4f.mul(offsetMatrix, rotMatrix, offsetMatrix);

		// float kekZERBORDEL_TARACE = 0.1f;
		// offsetMatrix.m31 += -kekZERBORDEL_TARACE;

		Matrix4f.mul(matrix, offsetMatrix, matrix);

		// matrix.m30 += offsetTotal.x;
		// matrix.m31 += -kekZERBORDEL_TARACE+0.0;
		matrix.m31 += -0.0f;
		// matrix.m32 += offsetTotal.y;

		return matrix;
	}

	private void load(File file)
	{
		bones.clear();
		try
		{
			Bone currentBone = null;
			int readingFrame = 0;
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "";
			boolean readingMotion = false;
			boolean readingDest = false;

			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", ""); // We don't want the tabs.
				String[] items = line.split(" ");
				if (readingMotion)
				{
					if (line.startsWith("Frames:"))
					{
						frames = Integer.parseInt(items[1]);
						root.recursiveInitMotion(frames);
					}
					else if (line.startsWith("Frame Time:"))
						frameTime = Float.parseFloat(items[2]);
					else
					{
						float[] lineData = new float[items.length];
						// Fill this shit
						for (int i = 0; i < lineData.length; i++)
							lineData[i] = Float.parseFloat(items[i]);
						// System.out.println("lineData : "+lineData.length+" / "+totalChannels);
						int index = 0;
						currentBone = root;
						while (currentBone != null)
						{
							if (!currentBone.animationDataLoaded)
							{
								// if(readingFrame == 0)
								// System.out.println("Reading "+currentBone.channels+" for bone : "+currentBone.name+" starting at index:"+index);
								for (int i = 0; i < currentBone.channels; i++)
								{
									currentBone.animationData[readingFrame][i] = lineData[index];
									index++;
								}
							}
							currentBone.animationDataLoaded = true;
							boolean hasDoneAllChilds = true;
							for (Bone c : currentBone.childs)
							{
								if (!c.animationDataLoaded)
								{
									hasDoneAllChilds = false;
									currentBone = c;
									break;
								}
							}
							if (hasDoneAllChilds)
								currentBone = currentBone.parent;
						}
						root.recursiveResetFlag();
						readingFrame++;
					}
				}
				else
				{
					if (line.equals("HIERARCHY"))
					{
						readingMotion = false;
					}
					else if (line.startsWith("ROOT"))
					{
						root = new Bone(items[1], null);
						currentBone = root;
						// Add to global bones list
						bones.add(root);
					}
					else if (line.startsWith("{"))
					{
						// ignore
					}
					else if (line.startsWith("OFFSET"))
					{
						if (readingDest)
							currentBone.dest = new Vector3f(Float.parseFloat(items[1]), Float.parseFloat(items[2]), Float.parseFloat(items[3]));
						else
							currentBone.offset = new Vector3f(Float.parseFloat(items[1]), Float.parseFloat(items[2]), Float.parseFloat(items[3]));
					}
					else if (line.startsWith("CHANNELS"))
					{
						// Currently only support for XYZ POS+ROT and ROT
						// formats.
						currentBone.channels = Integer.parseInt(items[1]);
					}
					else if (line.startsWith("JOINT"))
					{
						Bone newBone = new Bone(items[1], currentBone);
						currentBone.childs.add(newBone);
						currentBone = newBone;
						// Add to global bones list
						bones.add(newBone);
					}
					else if (line.equals("End Site"))
					{
						readingDest = true;
					}
					else if (line.startsWith("}"))
					{
						// If we were reading an end site, stop
						if (readingDest)
							readingDest = false;
						else
							// Else we point to current bone's parent
							currentBone = currentBone.parent;
					}
					else if (line.equals("MOTION"))
					{
						readingMotion = true;
					}
				}
			}
			reader.close();
		}
		catch (IOException | NullPointerException e)
		{
			e.printStackTrace();
		}
	}

	public String toString()
	{
		String txt = "BVH ANIMATION FILE\n";
		txt += root.toString();
		txt += "Frames: " + frames + "\n";
		txt += "Frame Time: " + frameTime;
		return txt;
	}
}
