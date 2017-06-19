package io.xol.chunkstories.api.math.vector;

public interface Vector4m<T extends Number> extends Vector4<T>, Vector3m<T>
{
	// Members accessors
	
	/** Sets the X component */
	public Vector4m<T> setX(T value);
	
	/** Sets the Y component */
	public Vector4m<T> setY(T value);
	
	/** Sets the Z component */
	public Vector4m<T> setZ(T value);
	
	/** Sets the W component */
	public Vector4m<T> setW(T value);
	
	/** Sets all components at once */
	public Vector4m<T> set(T x, T y, T z, T w);
	
	// Type overload

	/** Flips each component of the vector */
	public Vector4m<T> negate();

	/** Divides each component of the vector by length(v0) */
	public Vector4m<T> normalize();

	/** Scales each component of the vector by a given factor */
	public Vector4m<T> scale(T scaleFactor);
	
	/** Returns a non-modifiable version of this vector */
	public Vector4<T> asImmutable();
	
	// Allowed operations
	
	/** Adds this vector to the components */
	public Vector4m<T> add(T a, T b, T c, T d);
	
	public Vector4m<T> add(Vector4m<T> vector);
	
	public Vector4m<T> sub(Vector4m<T> vector);
	
	public T dot(Vector4m<T> vector);
}