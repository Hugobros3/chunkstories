package io.xol.engine.math.lalgb.vector.dp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector4;
import io.xol.engine.math.lalgb.vector.Vector4m;
import io.xol.engine.math.lalgb.vector.abs.Vector4am;

public class Vector4dm extends Vector4am<Double>
{
	public Vector4dm()
	{
		this(0d);
	}
	
	public Vector4dm(double value)
	{
		this.x = value;
		this.y = value;
		this.z = value;
		this.w = value;
	}
	
	public Vector4dm(Vector4<?> vec)
	{
		this((double)vec.getX(), (double)vec.getY(), (double)vec.getZ(), (double)vec.getW());
	}
	
	public Vector4dm(double x, double y, double z, double w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	
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
	
	public Vector4dm clone()
	{
		return new Vector4dm(this);
	}

	@Override
	public Vector4m<Double> add(Double a, Double b, Double c, Double d)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		this.w += d;
		return this;
	}

	@Override
	public Vector4m<Double> add(Double a, Double b, Double c)
	{
		this.x += a;
		this.y += b;
		this.z += c;
		return this;
	}

	@Override
	public Vector4m<Double> add(Double a, Double b)
	{
		this.x += a;
		this.y += b;
		return this;
	}

	@Override
	public Double distanceTo(Vector4<Double> vector)
	{
		double dx = this.x - vector.getX();
		double dy = this.y - vector.getY();
		double dz = this.z - vector.getZ();
		double dw = this.w - vector.getW();
		return Math.sqrt(dx * dx + dy * dy + dz * dz + dw * dw);
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
	
	public Vector4dm castToDoublePrecision()
	{
		//No need to build objects
		return this;
	}

	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector4dm)
		{
			Vector4dm vec = (Vector4dm)object;
			return (double)vec.x == (double)x && (double)vec.y == (double)y && (double)vec.z == (double)z && (double)vec.w == (double)w;
		}
		//If it's sort of a vector
		else if(object instanceof Vector4<?>)
		{
			Vector4<Double> vec = ((Vector4<?>) object).castToDoublePrecision();
			return (double)vec.getX() == (double)x && (double)vec.getY() == (double)y && (double)vec.getZ() == (double)z && (double)vec.getW() == (double)w;
		}
		return false;
	}

}
