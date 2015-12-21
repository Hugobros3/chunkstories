package io.xol.engine.math.lalgb;

public abstract class Vector3<T extends Number> extends Vector<T>
{
	public T x;
	public T y;
	public T z;
	
	@Override
	public int getDimensions()
	{
		return 3;
	}

	@Override
	public double getLengthDouble()
	{
		double xd = x.doubleValue();
		double yd = y.doubleValue();
		double zd = z.doubleValue();
		return Math.sqrt(xd * xd + yd * yd + zd * zd);
	}

}
