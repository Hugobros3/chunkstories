package io.xol.engine.math.lalgb.vector.abs;

import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector2m;

public abstract class Vector2am<T extends Number> implements Vector2m<T>
{
	protected T x;
	protected T y;
	
	@Override
	public T getX()
	{
		return x;
	}
	
	@Override
	public T getY()
	{
		return y;
	}
	
	@Override
	public Vector2am<T> setX(T value)
	{
		this.x = value;
		return this;
	}
	
	@Override
	public Vector2am<T> setY(T value)
	{
		this.y = value;
		return this;
	}
	
	@Override
	public Vector2m<T> set(T x, T y)
	{
		this.x = x;
		this.y = y;
		return this;
	}
	
	@Override
	public Vector2<T> asImmutable()
	{
		return this;
	}
}
