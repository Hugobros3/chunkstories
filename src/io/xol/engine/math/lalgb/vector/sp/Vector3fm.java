package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.abs.Vector3am;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

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
	
	public <N extends Number> Vector3fm(Vector3<N> vec)
	{
		this(vec.getX(), vec.getY(), vec.getZ());
	}
	
	public <N extends Number> Vector3fm(N x, N y, N z)
	{
		if(x instanceof Double)
		{
			this.x = (float)(double)x;
			this.y = (float)(double)y;
			this.z = (float)(double)z;
		}
		else
		{
			this.x = (float)x;
			this.y = (float)y;
			this.z = (float)z;
		}
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

	@Override
	public Vector3fm add(Float a, Float b, Float c)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		return this;
	}

	@Override
	public Vector3fm add(Float a, Float b)
	{
		this.x += a;
		this.y += b;
		return this;
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
	
	public Vector3fm castToSinglePrecision()
	{
		//No need to build objects
		return this;
	}

	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector3fm)
		{
			Vector3fm vec = (Vector3fm)object;
			return (float)vec.x == (float)x && (float)vec.y == (float)y && (float)vec.z == (float)z;
		}
		//If it's sort of a vector
		else if(object instanceof Vector3<?>)
		{
			Vector3<Float> vec = ((Vector3<?>) object).castToSinglePrecision();
			return (float)vec.getX() == (float)x && (float)vec.getY() == (float)y && (float)vec.getZ() == (float)z;
		}
		return false;
	}

	@Override
	public Vector3dm castToDoublePrecision()
	{
		return new Vector3dm(x, y, z);
	}

}
