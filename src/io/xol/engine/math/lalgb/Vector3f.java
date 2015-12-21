package io.xol.engine.math.lalgb;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector3f
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector
	
	public float x, y, z;

	public Vector3f()
	{
		this(0, 0, 0);
	}

	public Vector3f(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3f add(Vector3f b)
	{
		x += b.x;
		y += b.y;
		z += b.z;
		return this;
	}

	public Vector3f sub(Vector3f b)
	{
		x -= b.x;
		y -= b.y;
		z -= b.z;
		return this;
	}

	public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		dest.add(left);
		dest.add(right);
		return dest;
	}

	public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		dest.add(left);
		dest.sub(right);
		return dest;
	}

	public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		dest.x = left.y * right.z - left.z * right.y;
		dest.y = left.x * right.z - left.z * right.x;
		dest.z = left.x * right.y - left.y * right.x;
		return dest;
	}

	public static float dot(Vector3f left, Vector3f right)
	{
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}

	public static float angle(Vector3f left, Vector3f right)
	{
		float normalizedDot = dot(left, right) / (left.length() * right.length());
		if (normalizedDot < -1f)
			normalizedDot = -1f;
		if (normalizedDot > 1f)
			normalizedDot = 1f;
		return (float) Math.acos(normalizedDot);
	}

	public Vector3f negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		return this;
	}

	public Vector3f scale(float s)
	{
		this.x *= s;
		this.y *= s;
		this.z *= s;
		return this;
	}

	public Vector3f normalize()
	{
		float length = length();
		x /= length;
		y /= length;
		z /= length;
		return this;
	}

	public float length()
	{
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public float lengthSquared()
	{
		return x * x + y * y + z * z;
	}
	
	public Vector3d castToDP()
	{
		Vector3d vec = new Vector3d();
		vec.x = x;
		vec.y = y;
		vec.z = z;
		return vec;
	}

}
