package io.xol.engine.math.lalgb;

import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Vector3d
{
	// Dirty self-made replacement for vecmatch classes, castable to LWJGL's vector
	protected double x;
	protected double y;
	protected double z;

	public Vector3d()
	{
		this(0, 0, 0);
	}

	public Vector3d(double x, double y, double z)
	{
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	public Vector3d(Vector3d vec)
	{
		this.setX(vec.getX());
		this.setY(vec.getY());
		this.setZ(vec.getZ());
	}

	public Vector3d(double d)
	{
		this(d, d, d);
	}

	public double getX()
	{
		return x;
	}

	public void setX(double x)
	{
		this.x = x;
	}

	public double getY()
	{
		return y;
	}

	public void setY(double y)
	{
		this.y = y;
	}

	public double getZ()
	{
		return z;
	}

	public void setZ(double z)
	{
		this.z = z;
	}

	public Vector3d add(Vector3d b)
	{
		setX(getX() + b.getX());
		setY(getY() + b.getY());
		setZ(getZ() + b.getZ());
		return this;
	}

	public Vector3d sub(Vector3d b)
	{
		setX(getX() - b.getX());
		setY(getY() - b.getY());
		setZ(getZ() - b.getZ());
		return this;
	}

	public void set(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/*public static Vector3d add(Vector3d left, Vector3d right, Vector3d dest)
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
		dest.setX(left.getY() * right.getZ() - left.getZ() * right.getY());
		dest.setY(left.getZ() * right.getX() - left.getX() * right.getZ());
		dest.setZ(left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}

	public static Vector3d cross(Vector3d left, Vector3d right)
	{
		Vector3d dest = new Vector3d();
		dest.setX(left.getY() * right.getZ() - left.getZ() * right.getY());
		dest.setY(left.getZ() * right.getX() - left.getX() * right.getZ());
		dest.setZ(left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}*/

	/*public static double dot(Vector3d left, Vector3d right)
	{
		return left.getX() * right.getX() + left.getY() * right.getY() + left.getZ() * right.getZ();
	}*/
	
	public double dot(Vector3d right)
	{
		return this.getX() * right.getX() + this.getY() * right.getY() + this.getZ() * right.getZ();
	}

	/*public static double angle(Vector3d left, Vector3d right)
	{
		double normalizedDot = dot(left, right) / (left.length() * right.length());
		if (normalizedDot < -1d)
			normalizedDot = -1d;
		if (normalizedDot > 1d)
			normalizedDot = 1d;
		return Math.acos(normalizedDot);
	}*/

	public Vector3d negate()
	{
		this.setX(-getX());
		this.setY(-getY());
		this.setZ(-getZ());
		return this;
	}

	public Vector3d scale(double s)
	{
		this.setX(this.getX() * s);
		this.setY(this.getY() * s);
		this.setZ(this.getZ() * s);
		return this;
	}

	public Vector3d normalize()
	{
		double length = length();
		setX(getX() / length);
		setY(getY() / length);
		setZ(getZ() / length);
		return this;
	}

	public double length()
	{
		return Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
	}

	public double lengthSquared()
	{
		return getX() * getX() + getY() * getY() + getZ() * getZ();
	}

	public Vector3f castToSimplePrecision()
	{
		Vector3f vec = new Vector3f();
		vec.setX((float) getX());
		vec.setY((float) getY());
		vec.setZ((float) getZ());
		return vec;
	}

	public void set(Vector3d v)
	{
		this.setX(v.getX());
		this.setY(v.getY());
		this.setZ(v.getZ());
	}

	@Override
	public String toString()
	{
		return "[Vector3d x:" + getX() + " y:" + getY() + " z:" + getZ() + "]";
	}

	public Vector3d zero()
	{
		this.setX(0d);
		this.setY(0d);
		this.setZ(0d);
		return this;
	}

	public Vector3d add(double x, double y, double z)
	{
		this.setX(this.getX() + x);
		this.setY(this.getY() + y);
		this.setZ(this.getZ() + z);
		return this;
	}

	public double distanceTo(Vector3d vec)
	{
		return Math.sqrt((getX() - vec.getX()) * (getX() - vec.getX()) + (getY() - vec.getY()) * (getY() - vec.getY()) + (getZ() - vec.getZ()) * (getZ() - vec.getZ()));
	}

	public Vector3d clone()
	{
		return new Vector3d(getX(), getY(), getZ());
	}
	
	public boolean equals(Object o)
	{
		if(!(o instanceof Vector3d))
			return false;
		
		Vector3d vec = (Vector3d)o;
		return vec.x == x && vec.y == y && vec.z == z;
	}
	
	public int hashCode()
	{
		return 1024 * (int)x + 32 * (int)y + (int)z;
	}
}