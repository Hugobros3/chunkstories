package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector4;
import io.xol.engine.math.lalgb.vector.Vector4m;
import io.xol.engine.math.lalgb.vector.abs.Vector4am;

public class Vector4fm extends Vector4am<Float>
{
	public Vector4fm()
	{
		this(0f);
	}
	
	public Vector4fm(double value)
	{
		this((float)value);
	}
	
	public Vector4fm(float value)
	{
		this.x = value;
		this.y = value;
		this.z = value;
		this.w = value;
	}
	
	public Vector4fm(Vector4<?> vec)
	{
		this((float)vec.getX(), (float)vec.getY(), (float)vec.getZ(), (float)vec.getW());
	}
	
	public Vector4fm(float x, float y, float z, float w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public Vector4fm(double x, double y, double z, double w)
	{
		this.x = (float)x;
		this.y = (float)y;
		this.z = (float)z;
		this.w = (float)w;
	}
	
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
	
	public Vector4fm clone()
	{
		return new Vector4fm(this);
	}

	@Override
	public Vector4m<Float> add(Float a, Float b, Float c, Float d)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		this.w += d;
		return this;
	}

	@Override
	public Vector4m<Float> add(Float a, Float b, Float c)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		return this;
	}

	@Override
	public Vector4m<Float> add(Float a, Float b)
	{
		this.x += a;
		this.y += b;
		return this;
	}

	@Override
	public Float distanceTo(Vector4<Float> vector)
	{
		float dx = this.x - vector.getX();
		float dy = this.y - vector.getY();
		float dz = this.z - vector.getZ();
		float dw = this.w - vector.getW();
		return (float)Math.sqrt(dx * dx + dy * dy + dz * dz + dw * dw);
	}

	@Override
	public Float distanceTo(Vector3<Float> vector)
	{
		float dx = this.x - vector.getX();
		float dy = this.y - vector.getY();
		float dz = this.z - vector.getZ();
		return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public Float distanceTo(Vector2<Float> vector)
	{
		float dx = this.x - vector.getX();
		float dy = this.y - vector.getY();
		return (float)Math.sqrt(dx * dx + dy * dy);
	}
	
	public Vector4fm castToSinglePrecision()
	{
		//No need to build objects
		return this;
	}

	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector4fm)
		{
			Vector4fm vec = (Vector4fm)object;
			return vec.x == x && vec.y == y && vec.z == z && vec.w == w;
		}
		//If it's sort of a vector
		else if(object instanceof Vector4<?>)
		{
			Vector4<Float> vec = ((Vector4<?>) object).castToSinglePrecision();
			return vec.getX() == x && vec.getY() == y && vec.getZ() == z && vec.getW() == w;
		}
		return false;
	}

}
