//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.animation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import xyz.chunkstories.api.animation.Animation;
import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.math.Quaternion4d;

public class BVHAnimation implements Animation {
	int frames = 0;
	float frameTime = 0f;

	BVHTreeBone root;
	private Map<String, BVHTreeBone> bones = new HashMap<String, BVHTreeBone>();

	public static void main(String a[]) throws FileNotFoundException {
		BVHAnimation test = new BVHAnimation(new FileInputStream(new File("res/animations/human/human.bvh")));
		System.out.println(test.toString());

		float rotX = (float) (Math.random() * 2.0 * Math.PI);
		float rotY = (float) (Math.random() * 2.0 * Math.PI);
		float rotZ = (float) (Math.random() * 2.0 * Math.PI);

		Quaternion4d quaternionXLower = Quaternion4d.fromAxisAngle(new Vector3d(1.0, 0.0, 0.0), rotX);
		Quaternion4d quaternionYLower = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 1.0, 0.0), rotY);
		Quaternion4d quaternionZLower = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 0.0, 1.0), rotZ);
		Quaternion4d total = new Quaternion4d(quaternionXLower);
		total = total.mult(quaternionYLower);
		total.normalize();
		total = total.mult(quaternionZLower);

		Matrix4f matrix = new Matrix4f();

		// Apply rotations
		matrix.rotate(rotX, new Vector3f(1, 0, 0));
		matrix.rotate(rotY, new Vector3f(0, 1, 0));
		matrix.rotate(rotZ, new Vector3f(0, 0, 1));

		Matrix4f mX = new Matrix4f();
		Matrix4f mY = new Matrix4f();
		Matrix4f mZ = new Matrix4f();

		mX.rotate(rotX, new Vector3f(1, 0, 0));
		mY.rotate(rotY, new Vector3f(0, 1, 0));
		mZ.rotate(rotZ, new Vector3f(0, 0, 1));

		/*
		 * System.out.println("Old:\n"+matrix);
		 * System.out.println("New:\n"+Matrix4f.mul(Matrix4f.mul(mX, mY, null), mZ,
		 * null));
		 * 
		 * System.out.println("mX:\n"+mX); System.out.println("mY:\n"+mY);
		 * System.out.println("mZ:\n"+mZ);
		 */

		// System.out.println(quaternionXLower);
		// System.out.println(quaternionYLower);
		// System.out.println(quaternionZLower);

		mX = Quaternion4d.fromAxisAngle(new Vector3d(1.0, 0.0, 0.0), rotX).toMatrix4f();
		mY = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 1.0, 0.0), rotY).toMatrix4f();
		mZ = Quaternion4d.fromAxisAngle(new Vector3d(0.0, 0.0, 1.0), rotZ).toMatrix4f();

		// System.out.println("Old:\n"+matrix);
		// System.out.println("New:\n"+Matrix4f.mul(Matrix4f.mul(mX, mY, null), mZ,
		// null));
		// System.out.println("mX:\n"+mX);
		// System.out.println("mY:\n"+mY);
		// System.out.println("mZ:\n"+mZ);

		// total = Matrix4f.invert(total, null);

		// System.out.println("Qml:\n"+total.toMatrix4f());
		// System.out.println("Inv:\n"+Matrix4f.invert(total.toMatrix4f(), null));

		// Swaps Y and Z axises arround
		// matrix = new Matrix4f();

		System.out.println(matrix);

		System.out.println("");

		Matrix4f blender2ingame = new Matrix4f();
		blender2ingame.m11(0);
		blender2ingame.m22(0);
		blender2ingame.m12(1);
		blender2ingame.m21(1);

		// Rotate the matrix first to apply the transformation in blender space

		blender2ingame.mul(matrix, matrix);
		// Matrix4f.mul(blender2ingame, matrix, matrix);

		// Mirror it
		Matrix4f mirror = new Matrix4f();
		mirror.m22(-1);

		mirror.mul(matrix, matrix);
		// Matrix4f.mul(mirror, matrix, matrix);

		// Rotate again after so it's back the correct way arround

		matrix.mul(blender2ingame);
		// Matrix4f.mul(matrix, blender2ingame, matrix);

		matrix.mul(mirror);
		// Matrix4f.mul(matrix, mirror, matrix);

		System.out.println(matrix);
	}

	public BVHAnimation(Asset asset) {
		load(asset.read());
	}

	public BVHAnimation(FileInputStream fileInputStream) {
		load(fileInputStream);
	}

	/*
	 * public Matrix4f getBoneHierarchyTransformationMatrix(String boneName, double
	 * animationTime) { BVHTreeBone bone = getBone(boneName); return
	 * bone.getTransformationMatrix(animationTime); }
	 */

	public Matrix4f getBoneHierarchyTransformationMatrix(String boneName, double animationTime) {
		Matrix4f matrix = new Matrix4f();
		if (frames == 0) {
			System.out.println("fack you");
			return matrix;
		}

		double frame = animationTime / 1000.0 / frameTime;

		double frameUpperBound = Math.ceil(frame);
		double frameLowerBound = Math.floor(frame);

		double interp = frame % 1.0;
		// Don't try to interpolate if we're on an exact frame
		if (frameLowerBound == frameUpperBound)
			interp = 0.0;

		int frameLower = (int) (frameLowerBound) % frames;
		int frameUpper = (int) (frameUpperBound) % frames;

		matrix = getBone(boneName).getTransformationMatrixInterpolatedRecursive(frameLower, frameUpper, interp);

		// System.out.println("lel unused");
		// transformBlenderBVHExportToChunkStoriesWorldSpace(matrix);

		return matrix;
	}

	public static Matrix4f transformBlenderBVHExportToChunkStoriesWorldSpace(Matrix4f matrix) {
		Matrix4f blender2ingame = new Matrix4f();
		// Cs & Blender conventions are both right-handed, ez way to map them is
		// Blender | Chunk Stories
		// +X      | +Z
		// +Y      | +X
		// +Z      | +Y
		blender2ingame.m00(0.0f);
		blender2ingame.m11(0.0f);
		blender2ingame.m22(0.0f);

		blender2ingame.m02(1.0f);
		blender2ingame.m10(1.0f);
		blender2ingame.m21(1.0f);

		// Rotate the matrix first to apply the transformation in blender space
		blender2ingame.mul(matrix, matrix);

		//Matrix4f out = new Matrix4f(blender2ingame);
		//out.invert();
		Matrix4f out = new Matrix4f();
		out.m00(0.0f);
		out.m11(0.0f);
		out.m22(0.0f);

		out.m20(1.0f);
		out.m01(1.0f);
		out.m12(1.0f);

		matrix.mul(out, matrix);

		return matrix;
	}

	public Matrix4f getOffsetMatrix(String boneName) {
		Matrix4f offsetMatrix = new Matrix4f();
		Vector3f offsetTotal = new Vector3f();

		BVHTreeBone bone = getBone(boneName);

		// Accumulate the transformation offset
		BVHTreeBone loop = bone;
		while (loop != null) {
			// Coordinates systems n stuff
			offsetTotal.x = (offsetTotal.x() + loop.offset.y());
			offsetTotal.y = (offsetTotal.y() + loop.offset.z());
			offsetTotal.z = (offsetTotal.z() + loop.offset.x());
			loop = loop.parent;
		}

		// Negate it and build the offset matrix
		offsetTotal.negate();
		offsetMatrix.m30(offsetMatrix.m30() + offsetTotal.x());
		offsetMatrix.m31(offsetMatrix.m31() + offsetTotal.y());
		offsetMatrix.m32(offsetMatrix.m32() + offsetTotal.z());

		return offsetMatrix;
	}

	public Matrix4f getBoneHierarchyTransformationMatrixWithOffset(String boneName, double animationTime) {
		Matrix4f matrix = null;
		if (frames == 0) {
			System.out.println("Invalid bone : " + boneName + "in animation" + this);
			return new Matrix4f();
		}

		// Get the normal bone transformation
		matrix = getBoneHierarchyTransformationMatrix(boneName, animationTime);

		// Apply the offset matrix
		matrix.mul(getOffsetMatrix(boneName));

		return matrix;
	}

	public BVHTreeBone getBone(String boneName) {
		BVHTreeBone bone = bones.get(boneName);
		return bone == null ? root : bone;
	}

	private void load(InputStream is) {
		bones.clear();
		try {
			BVHTreeBone currentBone = null;
			int readingFrame = 0;
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = "";
			boolean readingMotion = false;
			boolean readingDest = false;

			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", ""); // We don't want the tabs.
				String[] items = line.split(" ");
				if (readingMotion) {
					if (line.startsWith("Frames:")) {
						frames = Integer.parseInt(items[1]);
						root.recursiveInitMotion(frames);
					} else if (line.startsWith("Frame Time:"))
						frameTime = Float.parseFloat(items[2]);
					else {
						float[] lineData = new float[items.length];
						// Fill this shit
						for (int i = 0; i < lineData.length; i++)
							lineData[i] = Float.parseFloat(items[i]);
						// System.out.println("lineData : "+lineData.length+" / "+totalChannels);
						int index = 0;
						currentBone = root;
						while (currentBone != null) {
							if (!currentBone.animationDataLoaded) {
								// if(readingFrame == 0)
								// System.out.println("Reading "+currentBone.channels+" for bone :
								// "+currentBone.name+" starting at index:"+index);

								// For some reason I had a overly compilcated system but it just ends up reading
								// it sequencially it seems
								// TODO clean that crap up and simply shit
								for (int i = 0; i < currentBone.channels; i++) {
									currentBone.animationData[readingFrame][i] = lineData[index];
									// System.out.println("reading channel "+i+" for bone id"+currentBone.id);
									index++;
								}
							}
							currentBone.animationDataLoaded = true;
							boolean hasDoneAllChilds = true;
							for (BVHTreeBone c : currentBone.childs) {
								if (!c.animationDataLoaded) {
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
				} else {
					if (line.equals("HIERARCHY")) {
						readingMotion = false;
					} else if (line.startsWith("ROOT")) {
						final String boneName = items[1];
						root = new BVHTreeBone(boneName, null, this);
						currentBone = root;
						// Add to global bones list
						bones.put(boneName, root);
					} else if (line.startsWith("{")) {
						// ignore
					} else if (line.startsWith("OFFSET")) {
						if (readingDest)
							currentBone.dest = new Vector3f(Float.parseFloat(items[1]), Float.parseFloat(items[2]),
									Float.parseFloat(items[3]));
						else
							currentBone.offset = new Vector3f(Float.parseFloat(items[1]), Float.parseFloat(items[2]),
									Float.parseFloat(items[3]));
					} else if (line.startsWith("CHANNELS")) {
						// Currently only support for XYZ POS+ROT and ROT
						// formats.
						currentBone.channels = Integer.parseInt(items[1]);
					} else if (line.startsWith("JOINT")) {
						final String boneName = items[1];
						BVHTreeBone newBone = new BVHTreeBone(boneName, currentBone, this);
						currentBone.childs.add(newBone);
						currentBone = newBone;
						// Add to global bones list
						bones.put(boneName, newBone);
					} else if (line.equals("End Site")) {
						readingDest = true;
					} else if (line.startsWith("}")) {
						// If we were reading an end site, stop
						if (readingDest)
							readingDest = false;
						else
							// Else we point to current bone's parent
							currentBone = currentBone.parent;
					} else if (line.equals("MOTION")) {
						readingMotion = true;
					}
				}
			}
			reader.close();
		} catch (IOException | NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		String txt = "[BVH Animation File\n";
		if (root != null)
			txt += root.toString();
		txt += "Frames: " + frames + "\n";
		txt += "Frame Time: " + frameTime + "]";
		return txt;
	}
}
