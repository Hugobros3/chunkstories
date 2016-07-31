package io.xol.engine.math;

import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class to do crappy simple math
 */
public class Math2
{
	public static int floor(double x)
	{
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}
	
	public static double clampd(double a, double l, double u)
	{
		return Math.min(Math.max(a, l), u);
	}
	
	public static float clamp(double a, double l, double u)
	{
		return (float)clampd(a, l, u);
	}
	
	public static double mixd(double a, double b, double f)
	{
		return a * (1.0 - f) + b * f;
	}
	
	public static float mix(double a, double b, double f)
	{
		return (float)mixd(a, b, f);
	}
	
	public static Vector3f mix(Vector3f a, Vector3f b, double f)
	{
		Vector3f vec = new Vector3f();
		vec.x = mix(a.x, b.x, f);
		vec.y = mix(a.y, b.y, f);
		vec.z = mix(a.z, b.z, f);
		return vec;
	}
	
	public static Vector3d mix(Vector3d a, Vector3d b, double f)
	{
		Vector3d vec = new Vector3d();
		vec.setX(mix(a.getX(), b.getX(), f));
		vec.setY(mix(a.getY(), b.getY(), f));
		vec.setZ(mix(a.getZ(), b.getZ(), f));
		return vec;
	}
}
