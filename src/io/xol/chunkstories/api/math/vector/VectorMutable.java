package io.xol.chunkstories.api.math.vector;

/**
 * Mutable vectors are modifiable
 */

public interface VectorMutable<T extends Number>
{
	/** Flips each component of the vector */
	public VectorMutable<T> negate();
	
	/** Divides each component of the vector by length(v0) */
	public VectorMutable<T> normalize();
	
	/** Scales each component of the vector by a given factor */
	public VectorMutable<T> scale(T scaleFactor);
	
	/** Returns a non-modifiable version of this vector */
	public Vector<T> asImmutable();
}
