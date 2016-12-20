package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector4m;
import io.xol.engine.math.lalgb.vector.abs.Vector4am;

public class Vector4fm extends Vector4am<Float>
{

	@Override
	public Float length()
	{
		return (float) Math.sqrt(lengthSquared());
	}

	@Override
	public Float lengthSquared()
	{
		return x * x + y * y + z * z + w * w;
	}

	@Override
	public Vector4fm negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		this.w = -w;
		return this;
	}

	@Override
	public Vector4fm normalize()
	{
		return this.scale(1.0f / length());
	}

	@Override
	public Vector4fm scale(Float scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		this.z *= scaleFactor;
		this.w *= scaleFactor;
		return this;
	}

	@Override
	public Vector4m<Float> add(Vector4m<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		this.w += vector.getW();
		return this;
	}

	@Override
	public Vector4m<Float> sub(Vector4m<Float> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		this.z -= vector.getZ();
		this.w -= vector.getW();
		return this;
	}

	@Override
	public Float dot(Vector4m<Float> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY() + this.z * vector.getZ() + this.w * vector.getW();
	}
	
	/* 3d derivatives */
	
	@Override
	public Vector4fm add(Vector3<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		return this;
	}

	@Override
	public Vector4fm sub(Vector3<Float> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		this.z -= vector.getZ();
		return this;
	}

	@Override
	public Float dot(Vector3<Float> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY() + this.z * vector.getZ();
	}

	/* 2d derivatives */
	
	@Override
	public Vector4fm add(Vector2<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector4fm sub(Vector2<Float> vector)
	{
		this.x -= vector.getX();
		this.y -= vector.getY();
		return this;
	}

	@Override
	public Float dot(Vector2<Float> vector)
	{
		return this.x * vector.getX() + this.y * vector.getY();
	}

}
