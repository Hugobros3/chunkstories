package io.xol.chunkstories.api.math.vector.abs;

import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.Vector3m;

public abstract class Vector3am<T extends Number> extends Vector2am<T> implements Vector3m<T>
{
	protected T z;
	
	@Override
	public Vector3am<T> setX(T value)
	{
		this.x = value;
		return this;
	}
	
	@Override
	public Vector3am<T> setY(T value)
	{
		this.y = value;
		return this;
	}
	
	@Override
	public Vector3am<T> setZ(T value)
	{
		this.z = value;
		return this;
	}
	
	@Override
	public Vector3am<T> set(T x, T y, T z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	@Override
	public T getZ()
	{
		return z;
	}
	
	@Override
	public Vector3<T> asImmutable()
	{
		return this;
	}

	public String toString()
	{
		return "["+this.getClass().getSimpleName()+" x:"+getX()+" y:"+getY()+" z:"+getZ()+"]";
	}
}
