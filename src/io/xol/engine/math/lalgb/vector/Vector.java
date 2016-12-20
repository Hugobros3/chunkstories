package io.xol.engine.math.lalgb.vector;

public interface Vector<T extends Number>
{
	/** Returns the square root of the length() method */
	public T length();
	
	/** Returns the sum of the squares of the vector's component */
	public T lengthSquared();
	
	/** Returns a float-based representation of this vector */
	public Vector<Float> castToSinglePrecision();
	
	/** Returns a double-based representation of this vector */
	public Vector<Double> castToDoublePrecision();
	
	/** Returns a copy of the vector */
	//public Vector<T> clone();
}
