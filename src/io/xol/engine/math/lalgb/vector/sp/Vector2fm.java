package io.xol.engine.math.lalgb.vector.sp;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector2m;
import io.xol.engine.math.lalgb.vector.abs.Vector2am;
import io.xol.engine.math.lalgb.vector.dp.Vector2dm;

public class Vector2fm extends Vector2am<Float>
{
	public Vector2fm()
	{
		this(0f);
	}
	
	public Vector2fm(double value)
	{
		this((float)value);
	}
	
	public Vector2fm(float value)
	{
		this.x = value;
		this.y = value;
	}
	
	public Vector2fm(Vector2fm vec)
	{
		this((float)vec.getX(), (float)vec.getY());
	}
	
	public Vector2fm(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Vector2fm(double x, double y)
	{
		this.x = (float)x;
		this.y = (float)y;
	}
	
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
	
	public Vector2fm clone()
	{
		return new Vector2fm(this);
	}

	@Override
	public Vector2m<Float> add(Float a, Float b)
	{
		this.x += a;
		this.y += b;
		return this;
	}

	@Override
	public Float distanceTo(Vector2<Float> vector)
	{
		float dx = this.x - vector.getX();
		float dy = this.y - vector.getY();
		return (float)Math.sqrt(dx * dx + dy * dy);
	}
	
	public Vector2fm castToSinglePrecision()
	{
		//No need to build objects
		return this;
	}
	
	public Vector2dm castToDoublePrecision()
	{
		return new Vector2dm(this.getX(), this.getY());
	}

	public boolean equals(Object object)
	{
		//If it's the same kind of vector
		if(object instanceof Vector2fm)
		{
			Vector2fm vec = (Vector2fm)object;
			return (float)vec.x == (float)x && (float)vec.y == (float)y;
		}
		//If it's sort of a vector
		else if(object instanceof Vector2<?>)
		{
			Vector2<Float> vec = ((Vector2<?>) object).castToSinglePrecision();
			return (float)vec.getX() == (float)x && (float)vec.getY() == (float)y;
		}
		return false;
	}

}
