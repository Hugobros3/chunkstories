package io.xol.chunkstories.api.math.vector;

public interface Vector4<T extends Number> extends Vector3<T>
{
	/** Returns the W component */
	public T getW();
	
	/** Returns the distance to some vector */
	public T distanceTo(Vector4<T> vector);
	
	/** Returns a float-based representation of this vector */
	public Vector4<Float> castToSinglePrecision();
	
	/** Returns a double-based representation of this vector */
	public Vector4<Double> castToDoublePrecision();
}
