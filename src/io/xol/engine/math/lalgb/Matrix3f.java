package io.xol.engine.math.lalgb;

import java.nio.FloatBuffer;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Matrix3f
{
	// Hopefully hi-performance matrix class
	public float m00 = 1f;
	public float m01 = 0f;
	public float m02 = 0f;
	public float m10 = 0f;
	public float m11 = 1f;
	public float m12 = 0f;
	public float m20 = 0f;
	public float m21 = 0f;
	public float m22 = 1f;

	public Matrix3f()
	{

	}

	public Matrix3f setIdentity()
	{
		m00 = 1f;
		m01 = 0f;
		m02 = 0f;
		m10 = 0f;
		m11 = 1f;
		m12 = 0f;
		m20 = 0f;
		m21 = 0f;
		m22 = 1f;
		return this;
	}

	public Matrix3f store(FloatBuffer buf)
	{
		assert buf.capacity() == 9;
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		return this;
	}

	//From LWJGL
	public static Matrix3f invert(Matrix3f src, Matrix3f dest)
	{
		float determinant = src.determinant();

		if (determinant != 0)
		{
			if (dest == null)
				dest = new Matrix3f();
			/* do it the ordinary way
			 *
			 * inv(A) = 1/det(A) * adj(T), where adj(T) = transpose(Conjugate Matrix)
			 *
			 * m00 m01 m02
			 * m10 m11 m12
			 * m20 m21 m22
			 */
			float determinant_inv = 1f / determinant;

			// get the conjugate matrix
			float t00 = src.m11 * src.m22 - src.m12 * src.m21;
			float t01 = -src.m10 * src.m22 + src.m12 * src.m20;
			float t02 = src.m10 * src.m21 - src.m11 * src.m20;
			float t10 = -src.m01 * src.m22 + src.m02 * src.m21;
			float t11 = src.m00 * src.m22 - src.m02 * src.m20;
			float t12 = -src.m00 * src.m21 + src.m01 * src.m20;
			float t20 = src.m01 * src.m12 - src.m02 * src.m11;
			float t21 = -src.m00 * src.m12 + src.m02 * src.m10;
			float t22 = src.m00 * src.m11 - src.m01 * src.m10;

			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			return dest;
		}
		else
			return null;
	}

	public float determinant()
	{
		float f = m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
		return f;
	}
}
