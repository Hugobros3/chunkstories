package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.abs.Vector2am;

public class Vector2fm extends Vector2am<Float>
{
	@Override
	public Float length()
	{
		return (float) Math.sqrt(lengthSquared());
	}

	@Override
	public Float lengthSquared()
	{
		return x * x + y * y;
	}

	@Override
	public Vector2fm negate()
	{
		this.x = -x;
		this.y = -y;
		return this;
	}

	@Override
	public Vector2fm normalize()
	{
		return this.scale(1.0f / this.length());
	}

	@Override
	public Vector2fm scale(Float scaleFactor)
	{
		this.x *= scaleFactor;
		this.y *= scaleFactor;
		return this;
	}

	@Override
	public Vector2fm add(Vector2<Float> vector)
	{
		this.x += vector.getX();
		this.y += vector.getY();
		return this;
	}

	@Override
	public Vector2fm sub(Vector2<Float> vector)
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
