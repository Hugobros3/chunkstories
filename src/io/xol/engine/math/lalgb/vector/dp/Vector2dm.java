package io.xol.engine.math.lalgb.vector.dp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.abs.Vector2am;

public class Vector2dm extends Vector2am<Double>
{
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

}
