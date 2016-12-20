package io.xol.engine.math.lalgb.vector;

public interface Vector<T extends Number>
{
	/** Returns the square root of the length() method */
	public T length();
	
	/** Returns the sum of the squares of the vector's component */
	public T lengthSquared();
}
