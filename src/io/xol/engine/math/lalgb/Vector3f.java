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
	
	public Vector3f(double v)
	{
		this((float)v);
	}
	
	public Vector3f(float v)
	{
		this(v, v, v);
	}

	public Vector3f(double x, double y, double z)
	{
		this((float)x, (float)y, (float)z);
	}
	
	public Vector3f(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3f(Vector3f vec3)
	{
		this.x = vec3.x;
		this.y = vec3.y;
		this.z = vec3.z;
	}

	public void set(float x, float y, float z)
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
		dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
		//dest.add(left);
		//dest.add(right);
		return dest;
	}

	public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		//dest.set(0, 0, 0);
		dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
		//dest.add(left);
		//dest.sub(right);
		return dest;
	}

	public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		dest.set(
				left.y * right.z - left.z * right.y,
				right.x * left.z - right.z * left.x,
				left.x * right.y - left.y * right.x
				);
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

	public Vector3f negate(Vector3f out)
	{
		if (out == null)
			out = new Vector3f();
		out.x = -x;
		out.y = -y;
		out.z = -z;
		return out;
	}

	public Vector3f scale(float s)
	{
		this.x *= s;
		this.y *= s;
		this.z *= s;
		return this;
	}

	public Vector3f normalise(Vector3f destination)
	{
		return normalize(destination);
	}

	public Vector3f normalize(Vector3f destination)
	{
		float length = length();
		if(destination == null)
			return new Vector3f(x / length, y / length, z / length);
		destination.set(x / length, y / length, z / length);
		return destination;
	}

	public Vector3f normalise()
	{
		return normalize();
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
	
	public Vector3f clone()
	{
		return new Vector3f(x, y, z);
	}

	public String toString()
	{
		return "[Vector3f x:"+x+" y:"+y+" z:"+z+"]";
	}
}
