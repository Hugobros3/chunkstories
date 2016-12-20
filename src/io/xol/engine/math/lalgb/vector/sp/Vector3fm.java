package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.abs.Vector3am;

public class Vector3fm extends Vector3am<Float>
{
	public Vector3fm()
	{
		this(0f);
	}
	
	public Vector3fm(double value)
	{
		this((float)value);
	}
	
	public Vector3fm(float value)
	{
		this.x = value;
		this.y = value;
		this.z = value;
	}
	
	public Vector3fm(Vector3<?> vec)
	{
		this((float)vec.getX(), (float)vec.getY(), (float)vec.getZ());
	}
	
	public Vector3fm(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3fm(double x, double y, double z)
	{
		this.x = (float)x;
		this.y = (float)y;
		this.z = (float)z;
	}
	
	@Override
	public Float length()
	{
		return (float) Math.sqrt(lengthSquared());
	}

	@Override
	public Float lengthSquared()
	{
		return x * x + y * y + z * z;
	}

	@Override
	public Vector3fm negate()
	{
		this.x = -x;
		this.y = -y;
		this.z = -z;
		return this;
	}

	@Override
	public Vector3fm normalize()
	{
		return this.scale(1.0f / length());
	}

	@Override
	public Vector3fm scale(Float scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		this.z *= scaleFactor;
		return this;
	}

	@Override
	public Vector3fm add(Vector3<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		this.z += vector.getZ();
		return this;
	}

	@Override
	public Vector3fm sub(Vector3<Float> vector)
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

	/* 2d derivatives*/
	
	@Override
	public Vector3fm add(Vector2<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector3fm sub(Vector2<Float> vector)
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
	
	public Vector3fm clone()
	{
		return new Vector3fm(this);
	}

	public String toString()
	{
		return "[Vector3fm x:"+getX()+" y:"+getY()+" z:"+getZ()+"]";
	}

}
