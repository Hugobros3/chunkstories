package io.xol.chunkstories.api.math.vector;

public interface Vector3<T extends Number> extends Vector2<T>
{
	/** Returns the Z component */
	public T getZ();

	/** Returns the distance to some vector */
	public T distanceTo(Vector3<T> vector);
	
	/** Returns a float-based representation of this vector */
	public Vector3<Float> castToSinglePrecision();
	
	/** Returns a double-based representation of this vector */
	public Vector3<Double> castToDoublePrecision();
}
