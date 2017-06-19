package io.xol.chunkstories.api.math.vector.dp;

import io.xol.chunkstories.api.math.vector.Vector2;
import io.xol.chunkstories.api.math.vector.Vector2m;
import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.abs.Vector3am;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;

public class Vector3dm extends Vector3am<Double>
{
	public Vector3dm()
	{
		this(0, 0, 0);
	}
	
	public Vector3dm(Vector3dm vec)
	{
		this(vec.x, vec.y, vec.z);
	}

	public Vector3dm(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3dm(double d)
	{
		this(d, d, d);
	}
	
	@Override
	public Double length()
	{
		return (double) Math.sqrt(lengthSquared());
	}

	@Override
	public Double lengthSquared()
	{
		return x * x + y * y + z * z;
	}

	@Override
	public Vector3dm negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		return this;
	}

	@Override
	public Vector3dm normalize()
	{
		return this.scale(1.0d / length());
	}

	@Override
	public Vector3dm scale(Double scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		this.z *= scaleFactor;
		return this;
	}

	@Override
	public Vector3dm add(Vector3<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		return this;
	}

	@Override
	public Vector3dm sub(Vector3<Double> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		this.z -= vector.getZ();
		return this;
	}

	@Override
	public Double dot(Vector3<Double> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY() + this.z * vector.getZ();
	}

	/* 2d derivatives*/
	
	@Override
	public Vector3dm add(Vector2<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector3dm sub(Vector2<Double> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		return this;
	}

	@Override
	public Double dot(Vector2<Double> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY();
	}
	
	public Vector3dm clone()
	{
		return new Vector3dm(this);
	}

	@Override
	public Vector3dm add(Double a, Double b, Double c)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		return this;
	}

	@Override
	public Vector2m<Double> add(Double a, Double b)
	{
		this.x += a;
		this.y += b;
		return this;
	}

	@Override
	public Double distanceTo(Vector3<Double> vector)
	{
		double dx = this.x - vector.getX();
		double dy = this.y - vector.getY();
		double dz = this.z - vector.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public Double distanceTo(Vector2<Double> vector)
	{
		double dx = this.x - vector.getX();
		double dy = this.y - vector.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public void set(Vector3dm vector)
	{
		this.x = vector.x;
		this.y = vector.y;
		this.z = vector.z;
	}
	
	public Vector3dm castToDoublePrecision()
	{
		//No need to build objects
		return this;
	}

	@Override
	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector3dm)
		{
			Vector3dm vec = (Vector3dm)object;
			return (double)vec.x == (double)x && (double)vec.y == (double)y && (double)vec.z == (double)z;
		}
		//If it's sort of a vector
		else if(object instanceof Vector3<?>)
		{
			Vector3<Double> vec = ((Vector3<?>) object).castToDoublePrecision();
			return (double)vec.getX() == (double)x && (double)vec.getY() == (double)y && (double)vec.getZ() == (double)z;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return (int) (x * 10000 + y * 100 + z);
	}

	@Override
	public Vector3fm castToSinglePrecision()
	{
		return new Vector3fm(x, y, z);
	}
}
