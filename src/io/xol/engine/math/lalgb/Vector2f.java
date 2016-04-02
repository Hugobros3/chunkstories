package io.xol.engine.math.lalgb;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector2f
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector
	
	public float x, y;

	public Vector2f()
	{
		this(0, 0);
	}

	public Vector2f(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Vector2f(Vector2f vec3)
	{
		this.x = vec3.x;
		this.y = vec3.y;
	}

	public Vector2f add(Vector2f b)
	{
		x += b.x;
		y += b.y;
		return this;
	}

	public Vector2f sub(Vector2f b)
	{
		x -= b.x;
		y -= b.y;
		return this;
	}

	public void set(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public static Vector2f add(Vector2f left, Vector2f right, Vector2f dest)
	{
		if (dest == null)
			dest = new Vector2f();
		dest.add(left);
		dest.add(right);
		return dest;
	}

	public static Vector2f sub(Vector2f left, Vector2f right, Vector2f dest)
	{
		if (dest == null)
			dest = new Vector2f();
		dest.add(left);
		dest.sub(right);
		return dest;
	}

	public Vector2f negate()
	{
		this.x = -x;
		this.y = -y;
		return this;
	}

	public Vector2f scale(float s)
	{
		this.x *= s;
		this.y *= s;
		return this;
	}
	
	public Vector2f normalise(Vector2f destination)
	{
		return normalize(destination);
	}

	public Vector2f normalize(Vector2f destination)
	{
		Vector2f dest = new Vector2f(this);
		float length = length();
		dest.x /= length;
		dest.y /= length;
		return dest;
	}
	
	public Vector2f normalise()
	{
		return normalize();
	}
	
	public Vector2f normalize()
	{
		float length = length();
		x /= length;
		y /= length;
		return this;
	}

	public float length()
	{
		return (float) Math.sqrt(x * x + y * y);
	}

	public float lengthSquared()
	{
		return x * x + y * y;
	}
	
	/*public Vector3d castToDP()
	{
		Vector3d vec = new Vector3d();
		vec.x = x;
		vec.y = y;
		return vec;
	}*/

}
