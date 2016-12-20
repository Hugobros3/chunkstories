package io.xol.engine.math.lalgb.vector.dp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.abs.Vector3am;

public class Vector3dm extends Vector3am<Double>
{
	public Vector3dm()
	{
		this(0, 0, 0);
	}

	public Vector3dm(double x, double y, double z)
	{
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	public Vector3dm(Vector3<?> vec)
	{
		this.setX((double)vec.getX());
		this.setY((double)vec.getY());
		this.setZ((double)vec.getZ());
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

}
