package io.xol.engine.math.lalgb;

public abstract class Vector<T extends Number>
{
	public abstract int getDimensions();
	
	public abstract void scale(float scale);
	
	public float getLengthFloat()
	{
		return (float) getLengthDouble();
	}

	public void normalize()
	{
		double l = this.getLengthDouble();
		this.scale((float) (1.0/l));
	}
	
	public abstract double getLengthDouble();
}
