package io.xol.engine.math.lalgb.vector.dp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector2m;
import io.xol.engine.math.lalgb.vector.abs.Vector2am;

public class Vector2dm extends Vector2am<Double>
{
	public Vector2dm()
	{
		this(0f);
	}
	
	public Vector2dm(double value)
	{
		this.x = value;
		this.y = value;
	}
	
	public Vector2dm(Vector2<?> vec)
	{
		this((float)vec.getX(), (float)vec.getY());
	}
	
	public Vector2dm(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	@Override
	public Double length()
	{
		return (double) Math.sqrt(lengthSquared());
	}

	@Override
	public Double lengthSquared()
	{
		return x * x + y * y;
	}

	@Override
	public Vector2dm negate()
	{
		this.x = -x;
		this.y = -y;
		return this;
	}

	@Override
	public Vector2dm normalize()
	{
		return this.scale(1.0d / this.length());
	}

	@Override
	public Vector2dm scale(Double scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		return this;
	}
	
	public Vector2dm clone()
	{
		return new Vector2dm(this);
	}

	@Override
	public Vector2dm add(Vector2<Double> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector2dm sub(Vector2<Double> vector)
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

	@Override
	public Vector2m<Double> add(Double a, Double b)
	{
		this.x += a;
		this.y += b;
		return this;
	}

	@Override
	public Double distanceTo(Vector2<Double> vector)
	{
		double dx = this.x - vector.getX();
		double dy = this.y - vector.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public Vector2dm castToDoublePrecision()
	{
		//No need to build objects
		return this;
	}

	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector2dm)
		{
			Vector2dm vec = (Vector2dm)object;
			return (double)vec.x == (double)x && (double)vec.y == (double)y;
		}
		//If it's sort of a vector
		else if(object instanceof Vector2<?>)
		{
			Vector2<Double> vec = ((Vector2<?>) object).castToDoublePrecision();
			return (double)vec.getX() == (double)x && (double)vec.getY() == (double)y;
		}
		return false;
	}

}
