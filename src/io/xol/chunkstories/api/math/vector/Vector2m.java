package io.xol.chunkstories.api.math.vector;

public interface Vector2m<T extends Number> extends Vector2<T>, VectorMutable<T>
{
	// Members accessors
	
	/** Sets the X component */
	public Vector2m<T> setX(T value);
	
	/** Sets the Y component */
	public Vector2m<T> setY(T value);
	
	/** Sets both components */
	public Vector2m<T> set(T x, T y);
	
	// Type overload

	/** Flips each component of the vector */
	public Vector2m<T> negate();

	/** Divides each component of the vector by length(v0) */
	public Vector2m<T> normalize();

	/** Scales each component of the vector by a given factor */
	public Vector2m<T> scale(T scaleFactor);
	
	/** Returns a non-modifiable version of this vector */
	public Vector2<T> asImmutable();
	
	// Allowed operations

	/** Adds this vector to the components */
	public Vector2m<T> add(T a, T b);
	
	public Vector2m<T> add(Vector2<T> vector);
	
	public Vector2m<T> sub(Vector2<T> vector);
	
	public T dot(Vector2<T> vector);
}
