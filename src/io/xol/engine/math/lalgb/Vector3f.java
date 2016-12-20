package io.xol.engine.math.lalgb;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector3f
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector

	private float x;
	private float y;
	private float z;

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
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	public Vector3f(Vector3f vec3)
	{
		this.setX(vec3.getX());
		this.setY(vec3.getY());
		this.setZ(vec3.getZ());
	}

	public float getX()
	{
		return x;
	}
	
	public void setX(double x)
	{
		this.x = (float)x;
	}
	
	public void setX(float x)
	{
		this.x = x;
	}

	public float getY()
	{
		return y;
	}

	public void setY(double y)
	{
		this.y = (float)y;
	}

	public void setY(float y)
	{
		this.y = y;
	}

	public float getZ()
	{
		return z;
	}

	public void setZ(double z)
	{
		this.z = (float)z;
	}

	public void setZ(float z)
	{
		this.z = z;
	}

	public void set(float x, float y, float z)
	{
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	public Vector3f add(Vector3f b)
	{
		setX(getX() + b.getX());
		setY(getY() + b.getY());
		setZ(getZ() + b.getZ());
		return this;
	}

	public Vector3f sub(Vector3f b)
	{
		setX(getX() - b.getX());
		setY(getY() - b.getY());
		setZ(getZ() - b.getZ());
		return this;
	}

	public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		dest.set(left.getX() + right.getX(), left.getY() + right.getY(), left.getZ() + right.getZ());
		//dest.add(left);
		//dest.add(right);
		return dest;
	}

	public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		//dest.set(0, 0, 0);
		dest.set(left.getX() - right.getX(), left.getY() - right.getY(), left.getZ() - right.getZ());
		//dest.add(left);
		//dest.sub(right);
		return dest;
	}
	
	public float dot(Vector3f right)
	{
		return this.getX() * right.getX() + this.getY() * right.getY() + this.getZ() * right.getZ();
	}

	public Vector3f negate()
	{
		this.setX(-getX());
		this.setY(-getY());
		this.setZ(-getZ());
		return this;
	}
	
	public Vector3f scale(float s)
	{
		this.setX(this.getX() * s);
		this.setY(this.getY() * s);
		this.setZ(this.getZ() * s);
		return this;
	}

	public Vector3f normalize()
	{
		float length = length();
		setX(getX() / length);
		setY(getY() / length);
		setZ(getZ() / length);
		return this;
	}

	public float length()
	{
		return (float) Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
	}

	public float lengthSquared()
	{
		return getX() * getX() + getY() * getY() + getZ() * getZ();
	}

	public Vector3d castToDP()
	{
		Vector3d vec = new Vector3d();
		vec.setX(getX());
		vec.setY(getY());
		vec.setZ(getZ());
		return vec;
	}
	
	public Vector3f clone()
	{
		return new Vector3f(getX(), getY(), getZ());
	}

	public String toString()
	{
		return "[Vector3f x:"+getX()+" y:"+getY()+" z:"+getZ()+"]";
	}
}
