package io.xol.engine.math.lalgb;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector4f
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector

	private float x;
	private float y;
	private float z;
	private float w;

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
		this(vec.getX(), vec.getY(), vec.getZ(), f);
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
	
	public boolean equals(Object o)
	{
		if(o instanceof Vector4f)
		{
			Vector4f v = (Vector4f)o;
			return x == v.x && y == v.y && z == v.z && w == v.w;
		}
		return false;
		
	}

	public String toString()
	{
		return "[Vector4f x:" + x + " y:" + y + " z:" + z + " w:" + w + "]";
	}

	public float getX()
	{
		return x;
	}

	public void setX(float x)
	{
		this.x = x;
	}

	public float getY()
	{
		return y;
	}

	public void setY(float y)
	{
		this.y = y;
	}

	public float getZ()
	{
		return z;
	}

	public void setZ(float z)
	{
		this.z = z;
	}

	public float getW()
	{
		return w;
	}

	public void setW(float w)
	{
		this.w = w;
	}
}
