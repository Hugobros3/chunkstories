package io.xol.engine.math;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;

//(c) 2015-2017 XolioWare Interactive
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
	
	public static Vector3fm mix(Vector3fm a, Vector3fm b, double f)
	{
		Vector3fm vec = new Vector3fm();
		vec.setX(mix(a.getX(), b.getX(), f));
		vec.setY(mix(a.getY(), b.getY(), f));
		vec.setZ(mix(a.getZ(), b.getZ(), f));
		return vec;
	}
	
	public static Vector3dm mix(Vector3dm a, Vector3dm b, double f)
	{
		Vector3dm vec = new Vector3dm();
		vec.setX(mixd(a.getX(), b.getX(), f));
		vec.setY(mixd(a.getY(), b.getY(), f));
		vec.setZ(mixd(a.getZ(), b.getZ(), f));
		return vec;
	}

	public static int clampi(int a, int min, int max)
	{
		if(a < min)
			return min;
		else if(a > max)
			return max;
		return a;
	}
}
