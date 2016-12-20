package io.xol.engine.math.lalgb.vector;

public interface Vector3m<T extends Number> extends Vector3<T>, Vector2m<T>
{
	// Members accessors
	
	/** Sets the X component */
	public Vector3m<T> setX(T value);
	
	/** Sets the Y component */
	public Vector3m<T> setY(T value);
	
	/** Sets the Z component */
	public Vector3m<T> setZ(T value);
	
	/** Sets all components at once */
	public Vector3m<T> set(T x, T y, T z);
	
	// Type overload

	/** Flips each component of the vector */
	public Vector3m<T> negate();

	/** Divides each component of the vector by length(v0) */
	public Vector3m<T> normalize();

	/** Scales each component of the vector by a given factor */
	public Vector3m<T> scale(T scaleFactor);
	
	/** Returns a non-modifiable version of this vector */
	public Vector3<T> asImmutable();
	
	// Allowed operations
	
	/** Adds this vector to the components */
	public Vector3m<T> add(T a, T b, T c);
	
	public Vector3m<T> add(Vector3<T> vector);
	
	public Vector3m<T> sub(Vector3<T> vector);
	
	public T dot(Vector3<T> vector);
}
