package io.xol.engine.math.lalgb;

public abstract class Vector2<T extends Number> extends Vector<T>
{
	public T x;
	public T y;
	
	@Override
	public int getDimensions()
	{
		return 2;
	}

	@Override
	public double getLengthDouble()
	{
		double xd = x.doubleValue();
		double yd = y.doubleValue();
		return Math.sqrt(xd * xd + yd * yd);
	}

}
