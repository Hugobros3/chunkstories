package io.xol.engine.math.lalgb;

import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector3d
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector

	public double x, y, z;

	public Vector3d()
	{
		this(0, 0, 0);
	}

	public Vector3d(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3d add(Vector3d b)
	{
		x += b.x;
		y += b.y;
		z += b.z;
		return this;
	}

	public Vector3d sub(Vector3d b)
	{
		x -= b.x;
		y -= b.y;
		z -= b.z;
		return this;
	}

	public static Vector3d add(Vector3d left, Vector3d right, Vector3d dest)
	{
		if (dest == null)
			dest = new Vector3d();
		dest.add(left);
		dest.add(right);
		return dest;
	}

	public static Vector3d sub(Vector3d left, Vector3d right, Vector3d dest)
	{
		if (dest == null)
			dest = new Vector3d();
		dest.add(left);
		dest.sub(right);
		return dest;
	}

	public static Vector3d cross(Vector3d left, Vector3d right, Vector3d dest)
	{
		if (dest == null)
			dest = new Vector3d();
		dest.x = left.y * right.z - left.z * right.y;
		dest.y = left.x * right.z - left.z * right.x;
		dest.z = left.x * right.y - left.y * right.x;
		return dest;
	}

	public static double dot(Vector3d left, Vector3d right)
	{
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}

	public static double angle(Vector3d left, Vector3d right)
	{
		double normalizedDot = dot(left, right) / (left.length() * right.length());
		if (normalizedDot < -1d)
			normalizedDot = -1d;
		if (normalizedDot > 1d)
			normalizedDot = 1d;
		return Math.acos(normalizedDot);
	}

	public Vector3d negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		return this;
	}

	public Vector3d scale(double s)
	{
		this.x *= s;
		this.y *= s;
		this.z *= s;
		return this;
	}

	public Vector3d normalize()
	{
		double length = length();
		x /= length;
		y /= length;
		z /= length;
		return this;
	}

	public double length()
	{
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double lengthSquared()
	{
		return x * x + y * y + z * z;
	}

	public Vector3f castToSP()
	{
		Vector3f vec = new Vector3f();
		vec.x = (float) x;
		vec.y = (float) y;
		vec.z = (float) z;
		return vec;
	}

	public void set(Vector3d v)
	{
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	public String toString()
	{
		return "[Vector3d x:" + x + " y:" + y + " z:" + z + "]";
	}

}
