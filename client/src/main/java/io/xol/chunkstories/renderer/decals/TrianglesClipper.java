package io.xol.chunkstories.renderer.decals;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Straight from the 6th gate of hell, forged in the shattered skulls of fresh babies, this code should not be messed with. Proceed at your own risk.
 */
public class TrianglesClipper
{
	private static Matrix4f toClipSpace;
	private static Matrix4f fromClipSpace;

	private static ByteBuffer out;

	public static synchronized int clipTriangles(ByteBuffer in, ByteBuffer out, Matrix4f rotationMatrix, Vector3dc originPosition, Vector3fc direction, Vector3dc size)
	{
		int actualCount = 0;

		toClipSpace = new Matrix4f(rotationMatrix);
		toClipSpace.translate(new Vector3f((float)originPosition.x(), (float)originPosition.y(), (float)originPosition.z()).negate());
		
		Matrix4f resize = new Matrix4f();
		resize.scale(new Vector3f(1 / (float)size.x(), 1 / (float)size.y(), 1));
		
		resize.transpose();
		//Matrix4f.transpose(resize, resize);

		resize.mul(toClipSpace);
		//Matrix4f.mul(resize, toClipSpace, toClipSpace);

		Matrix4f decal = new Matrix4f();
		decal.translate(new Vector3f(0.5f, 0.5f, 1.0f));

		decal.mul(toClipSpace);
		//Matrix4f.mul(decal, toClipSpace, toClipSpace);

		fromClipSpace = new Matrix4f();
		toClipSpace.invert(fromClipSpace);
		//fromClipSpace = Matrix4f.invert(toClipSpace, fromClipSpace);

		TrianglesClipper.out = out;

		while (in.hasRemaining())
		{
			Vector3f triVert1 = new Vector3f(in.getFloat(), in.getFloat(), in.getFloat());

			//Skip backward-facing tris
			Vector3f normal = new Vector3f(in.getFloat(), in.getFloat(), in.getFloat());
			if (normal.dot(direction) >= 0)
			{
				for (int i = 0; i < 6; i++)
					in.getFloat();
				continue;
			}

			Vector3f triVert2 = new Vector3f(in.getFloat(), in.getFloat(), in.getFloat());
			//Etc
			for (int i = 0; i < 3; i++)
				in.getFloat();
			Vector3f triVert3 = new Vector3f(in.getFloat(), in.getFloat(), in.getFloat());
			//Etc
			for (int i = 0; i < 3; i++)
				in.getFloat();

			actualCount += 3 * cull(triVert1, triVert2, triVert3);
		}
		
		return actualCount;
	}

	private static int cull(Vector3f vert1, Vector3f vert2, Vector3f vert3)
	{
		Vector4f tv1 = new Vector4f();
		toClipSpace.transform(new Vector4f(vert1.x(), vert1.y(), vert1.z(), 1.0f), tv1);
		//Matrix4f.transform(toClipSpace, new Vector4f(vert1.x(), vert1.y(), vert1.z(), 1.0f), null);
		Vector4f tv2 = new Vector4f();
		toClipSpace.transform(new Vector4f(vert2.x(), vert2.y(), vert2.z(), 1.0f), tv2);
		//Matrix4f.transform(toClipSpace, new Vector4f(vert2.x(), vert2.y(), vert2.z(), 1.0f), null);
		Vector4f tv3 = new Vector4f();
		toClipSpace.transform(new Vector4f(vert3.x(), vert3.y(), vert3.z(), 1.0f), tv3);
		//Matrix4f.transform(toClipSpace, new Vector4f(vert3.x(), vert3.y(), vert3.z(), 1.0f), null);

		if (tv1.x() < 0.0 && tv2.x() < 0.0 && tv3.x() < 0.0)
			return 0;
		if (tv1.y() < 0.0 && tv2.y() < 0.0 && tv3.y() < 0.0)
			return 0;
		if (tv1.x() > 1.0 && tv2.x() > 1.0 && tv3.x() > 1.0)
			return 0;
		if (tv1.y() > 1.0 && tv2.y() > 1.0 && tv3.y() > 1.0)
			return 0;
		return cullLeft(tv1, tv2, tv3);
	}

	private static int cullLeft(Vector4f vert1, Vector4f vert2, Vector4f vert3)
	{
		//Sort
		Vector4f v1, v2, v3;
		if (vert1.x() > vert2.x())
		{
			if (vert1.x() > vert3.x())
			{
				v3 = vert1;
				if (vert2.x() > vert3.x())
				{
					v2 = vert2;
					v1 = vert3;
				}
				else
				{
					v2 = vert3;
					v1 = vert2;
				}
			}
			else
			{
				v2 = vert1;
				if (vert2.x() > vert3.x())
				{
					v3 = vert2;
					v1 = vert3;
				}
				else
				{
					v3 = vert3;
					v1 = vert2;
				}
			}
		}
		else
		{
			if (vert2.x() > vert3.x())
			{
				v3 = vert2;
				if (vert1.x() > vert3.x())
				{
					v2 = vert1;
					v1 = vert3;
				}
				else
				{
					v2 = vert3;
					v1 = vert1;
				}
			}
			else
			{
				v2 = vert2;
				v3 = vert3;
				v1 = vert1;
			}
		}
		if (v1.x() <= v2.x() && v2.x() <= v3.x())
		{

		}
		else
		{
			System.out.println("fuck X");
			return 0;
		}

		//Actual culling here
		float border = 0.0f;
		//One point is clipping
		if (v1.x() < border && v2.x() > border && v3.x() > border)
		{
			float d2to1 = v2.x() - v1.x();
			float d2tb = v2.x();
			Vector4f v2to1 = new Vector4f(v1).sub(v2);
			v2to1.mul((d2tb) / d2to1);
			v2to1.add(v2);
			int t = cullRight(v2to1, v2, v3);
			
			float d3to1 = v3.x() - v1.x();
			float d3tb = v3.x();
			Vector4f v3to1 = new Vector4f(v1).sub(v3);
			v3to1.mul((d3tb) / d3to1);
			v3to1.add(v3);
			return t + cullRight(v2to1, v3, v3to1);
		}
		//Two points are
		else if (v1.x() < border && v2.x() < border && v3.x() > border)
		{
			float d3tb = v3.x();
			
			float d3to1 = v3.x() - v1.x();
			Vector4f v3to1 = new Vector4f(v1).sub(v3);
			v3to1.mul((d3tb) / d3to1);
			v3to1.add(v3);
			
			float d3to2 = v3.x() - v2.x();
			Vector4f v3to2 = new Vector4f(v2).sub(v3);
			v3to2.mul((d3tb) / d3to2);
			v3to2.add(v3);
			
			//System.out.println("v3to1"+v3to1);
			
			return cullRight(v3to1, v3, v3to2);
		}
		//All are
		else if (v1.x() < border && v2.x() < border && v3.x() < border)
		{
			//System.out.println("all out !");
			return 0;
		}
		//None are
		else
		{
			return cullRight(v1, v2, v3);
		}
	}

	private static int cullRight(Vector4f v1, Vector4f v2, Vector4f v3)
	{
		float border = 1.0f;
		//One point is clipping
		if (v1.x() < border && v2.x() < border && v3.x() > border)
		{
			//System.out.println("clipping...");
			//Continue the two segments up to the border
			float d2t3 = v3.x() - v2.x();
			float d2tb = border - v2.x();
			Vector4f v2to3 = new Vector4f(v3).sub(v2);
			v2to3.mul((d2tb) / d2t3);
			v2to3.add(v2);
			//System.out.println(v2to3+" is in of clip ("+v2to3.x+")");
			//other one

			float d1t3 = v3.x() - v1.x();
			float d1tb = border - v1.x();
			Vector4f v1to3 = new Vector4f(v3).sub(v1);
			v1to3.mul((d1tb) / d1t3);
			v1to3.add(v1);
			//System.out.println(v1to3+" is in of clip ("+v1to3.x+")");

			return cullTop(v1, v2, v1to3) + cullTop(v2to3, v2, v1to3);
		}
		//Two points are
		else if (v1.x() < border && v2.x() > border && v3.x() > border)
		{
			float d1t3 = v3.x() - v1.x();
			float d1tb = border - v1.x();
			Vector4f v1to3 = new Vector4f(v3).sub(v1);
			v1to3.mul((d1tb) / d1t3);
			v1to3.add(v1);
			//other one
			float d1t2 = v2.x() - v1.x();
			//float d1tb = border - v1.x;
			Vector4f v1to2 = new Vector4f(v2).sub(v1);
			v1to2.mul((d1tb) / d1t2);
			v1to2.add(v1);

			return cullTop(v1, v1to2, v1to3);
		}
		else if (v1.x() > border && v2.x() > border && v3.x() > border)
		{
			//System.out.println("all out !");
			return 0;
		}
		else
		{
			return cullTop(v1, v2, v3);
		}
	}

	private static int cullTop(Vector4f vert1, Vector4f vert2, Vector4f vert3)
	{
		Vector4f v1, v2, v3;
		if (vert1.y() > vert2.y())
		{
			if (vert1.y() > vert3.y())
			{
				v3 = vert1;
				if (vert2.y() > vert3.y())
				{
					v2 = vert2;
					v1 = vert3;
				}
				else
				{
					v2 = vert3;
					v1 = vert2;
				}
			}
			else
			{
				v2 = vert1;
				if (vert2.y() > vert3.y())
				{
					v3 = vert2;
					v1 = vert3;
				}
				else
				{
					v3 = vert3;
					v1 = vert2;
				}
			}
		}
		else
		{
			if (vert2.y() > vert3.y())
			{
				v3 = vert2;
				if (vert1.y() > vert3.y())
				{
					v2 = vert1;
					v1 = vert3;
				}
				else
				{
					v2 = vert3;
					v1 = vert1;
				}
			}
			else
			{
				v2 = vert2;
				v3 = vert3;
				v1 = vert1;
			}
		}
		if (v1.y() <= v2.y() && v2.y() <= v3.y())
		{

		}
		else
		{
			System.out.println("fuck Y" + v1 + v2 + v3);
			return 0;
		}
		//Actual culling here
		
		float border = 1.0f;
		//One point is clipping
		if (v1.y() < border && v2.y() < border && v3.y() > border)
		{
			//System.out.println("clipping...");
			//Continue the two segments up to the border
			float d2t3 = v3.y() - v2.y();
			float d2tb = border - v2.y();
			Vector4f v2to3 = new Vector4f(v3).sub(v2);
			v2to3.mul((d2tb) / d2t3);
			v2to3.add(v2);
			//System.out.println(v2to3+" is in of clip ("+v2to3.y+")");
			//other one

			float d1t3 = v3.y() - v1.y();
			float d1tb = border - v1.y();
			Vector4f v1to3 = new Vector4f(v3).sub(v1);
			v1to3.mul((d1tb) / d1t3);
			v1to3.add(v1);
			//System.out.println(v1to3+" is in of clip ("+v1to3.y+")");

			return cullBot(v1, v2, v1to3) + cullBot(v2to3, v2, v1to3);
		}
		//Two points are
		else if (v1.y() < border && v2.y() > border && v3.y() > border)
		{
			float d1t3 = v3.y() - v1.y();
			float d1tb = border - v1.y();
			Vector4f v1to3 = new Vector4f(v3).sub(v1);
			v1to3.mul((d1tb) / d1t3);
			v1to3.add(v1);
			//other one
			float d1t2 = v2.y() - v1.y();
			//float d1tb = border - v1.y;
			Vector4f v1to2 = new Vector4f(v2).sub(v1);
			v1to2.mul((d1tb) / d1t2);
			v1to2.add(v1);

			return cullBot(v1, v1to2, v1to3);
		}
		else if (v1.y() > border && v2.y() > border && v3.y() > border)
		{
			//System.out.println("all out !");
			return 0;
		}
		else
		{
			return cullBot(v1, v2, v3);
		}
	}

	private static int cullBot(Vector4f v1, Vector4f v2, Vector4f v3)
	{
		float border = 0.0f;
		//One point is clipping
		if (v1.y() < border && v2.y() > border && v3.y() > border)
		{
			float d2to1 = v2.y() - v1.y();
			float d2tb = v2.y();
			Vector4f v2to1 = new Vector4f(v1).sub(v2);
			v2to1.mul((d2tb) / d2to1);
			v2to1.add(v2);
			int t = cullDone(v2to1, v2, v3);
			
			float d3to1 = v3.y() - v1.y();
			float d3tb = v3.y();
			Vector4f v3to1 = new Vector4f(v1).sub(v3);
			v3to1.mul((d3tb) / d3to1);
			v3to1.add(v3);
			return t + cullDone(v2to1, v3, v3to1);
		}
		//Two points are
		else if (v1.y() < border && v2.y() < border && v3.y() > border)
		{
			float d3tb = v3.y();
			
			float d3to1 = v3.y() - v1.y();
			Vector4f v3to1 = new Vector4f(v1).sub(v3);
			v3to1.mul((d3tb) / d3to1);
			v3to1.add(v3);
			
			float d3to2 = v3.y() - v2.y();
			Vector4f v3to2 = new Vector4f(v2).sub(v3);
			v3to2.mul((d3tb) / d3to2);
			v3to2.add(v3);
			
			//System.out.println("v3to1"+v3to1);
			
			return cullDone(v3to1, v3, v3to2);
		}
		//All are
		else if (v1.y() < border && v2.y() < border && v3.y() < border)
		{
			return 0;
			//System.out.println("all out !");
		}
		//None are
		else
		{
			return cullDone(v1, v2, v3);
		}
	}

	private static int cullDone(Vector4f vert1, Vector4f vert2, Vector4f vert3)
	{
		out(vert1);
		out(vert2);
		out(vert3);
		
		return 1;
	}

	private static Vector4f transformedTemp = new Vector4f();
	
	private static void out(Vector4f transformMe)
	{
		//Wrap round out buffer
		if (out.position() == out.capacity())
			out.position(0);
		
		//out(vert, tm.x, tm.y);
		//Vector4f keke = Matrix4f.transform(fromClipSpace, tm, null);

		fromClipSpace.transform(transformMe, transformedTemp);
		
		out.putFloat((float) transformedTemp.x());
		out.putFloat((float) transformedTemp.y());
		out.putFloat((float) transformedTemp.z());

		out.putFloat(transformMe.x());
		out.putFloat(transformMe.y());
	}
}
