package io.xol.chunkstories.api.math.vector;

public interface Vector2<T extends Number> extends Vector<T>
{
	/** Returns the X component */
	public T getX();
	
	/** Returns the Y component */
	public T getY();
	
	/** Returns the distance to some vector */
	public T distanceTo(Vector2<T> vector);
	
	/** Returns a float-based representation of this vector */
	public Vector2<Float> castToSinglePrecision();
	
	/** Returns a double-based representation of this vector */
	public Vector2<Double> castToDoublePrecision();
}
