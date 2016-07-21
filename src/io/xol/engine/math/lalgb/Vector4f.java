package io.xol.engine.math.lalgb;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector4f
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector

	public float x, y, z, w;

	public Vector4f()
	{
		this(0, 0, 0, 0);
	}

	public Vector4f(double f)
	{
		this((float) f);
	}

	public Vector4f(float f)
	{
		this(f, f, f, f);
	}

	public Vector4f(Vector3f vec, double f)
	{
		this(vec.x, vec.y, vec.z, f);
	}

	public Vector4f(double x, double y, double z, double w)
	{
		this((float) x, (float) y, (float) z, (float) w);
	}

	public Vector4f(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Vector4f(Vector4f vec4)
	{
		this.x = vec4.x;
		this.y = vec4.y;
		this.z = vec4.z;
		this.w = vec4.w;
	}

	public Vector4f add(Vector4f b)
	{
		x += b.x;
		y += b.y;
		z += b.z;
		w += b.w;
		return this;
	}

	public Vector4f sub(Vector4f b)
	{
		x -= b.x;
		y -= b.y;
		z -= b.z;
		w -= b.w;
		return this;
	}

	public void set(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public static Vector4f add(Vector4f left, Vector4f right, Vector4f dest)
	{
		if (dest == null)
			dest = new Vector4f();
		dest.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
		//dest.add(left);
		//dest.add(right);
		return dest;
	}

	public static Vector4f sub(Vector4f left, Vector4f right, Vector4f dest)
	{
		if (dest == null)
			dest = new Vector4f();
		dest.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
		return dest;
	}

	public static float dot(Vector4f left, Vector4f right)
	{
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	public static float angle(Vector4f left, Vector4f right)
	{
		float normalizedDot = dot(left, right) / (left.length() * right.length());
		if (normalizedDot < -1f)
			normalizedDot = -1f;
		if (normalizedDot > 1f)
			normalizedDot = 1f;
		return (float) Math.acos(normalizedDot);
	}

	public Vector4f negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		this.w = -w;
		return this;
	}

	public Vector4f scale(float s)
	{
		this.x *= s;
		this.y *= s;
		this.z *= s;
		this.w *= s;
		return this;
	}

	public Vector4f normalize()
	{
		float length = length();
		x /= length;
		y /= length;
		z /= length;
		w /= length;
		return this;
	}

	public float length()
	{
		return (float) Math.sqrt(x * x + y * y + z * z + w * w);
	}

	public float lengthSquared()
	{
		return x * x + y * y + z * z + w * w;
	}

	public String toString()
	{
		return "[Vector4f x:" + x + " y:" + y + " z:" + z + " w:" + w + "]";
	}
}
