package io.xol.chunkstories.api.math.vector.abs;

import io.xol.chunkstories.api.math.vector.Vector4;
import io.xol.chunkstories.api.math.vector.Vector4m;

public abstract class Vector4am<T extends Number> extends Vector3am<T> implements Vector4m<T>
{
	protected T w;
	
	@Override
	public Vector4am<T> setX(T value)
	{
		this.x = value;
		return this;
	}
	
	@Override
	public Vector4am<T> setY(T value)
	{
		this.y = value;
		return this;
	}
	
	@Override
	public Vector4am<T> setZ(T value)
	{
		this.z = value;
		return this;
	}
	
	@Override
	public Vector4am<T> setW(T value)
	{
		this.w = value;
		return this;
	}
	
	@Override
	public Vector4am<T> set(T x, T y, T z, T w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		return this;
	}

	@Override
	public T getW()
	{
		return w;
	}
	
	@Override
	public Vector4<T> asImmutable()
	{
		return this;
	}

	public String toString()
	{
		return "["+this.getClass().getSimpleName()+" x:"+getX()+" y:"+getY()+" z:"+getZ()+" w:"+getW()+"]";
	}
}
