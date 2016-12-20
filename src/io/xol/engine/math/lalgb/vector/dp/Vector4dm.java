package io.xol.engine.math.lalgb.vector.dp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector4m;
import io.xol.engine.math.lalgb.vector.abs.Vector4am;

public class Vector4dm extends Vector4am<Double>
{

	@Override
	public Double length()
	{
		return (double) Math.sqrt(lengthSquared());
	}

	@Override
	public Double lengthSquared()
	{
		return x * x + y * y + z * z + w * w;
	}

	@Override
	public Vector4dm negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		this.w = -w;
		return this;
	}

	@Override
	public Vector4dm normalize()
	{
		return this.scale(1.0d / length());
	}

	@Override
	public Vector4dm scale(Double scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		this.z *= scaleFactor;
		this.w *= scaleFactor;
		return this;
	}

	@Override
	public Vector4m<Double> add(Vector4m<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		this.w += vector.getW();
		return this;
	}

	@Override
	public Vector4m<Double> sub(Vector4m<Double> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		this.z -= vector.getZ();
		this.w -= vector.getW();
		return this;
	}

	@Override
	public Double dot(Vector4m<Double> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY() + this.z * vector.getZ() + this.w * vector.getW();
	}
	
	/* 3d derivatives */
	
	@Override
	public Vector4dm add(Vector3<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		return this;
	}

	@Override
	public Vector4dm sub(Vector3<Double> vector)
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

	/* 2d derivatives */
	
	@Override
	public Vector4dm add(Vector2<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector4dm sub(Vector2<Double> vector)
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
