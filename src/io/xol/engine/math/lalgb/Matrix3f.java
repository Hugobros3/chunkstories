package io.xol.engine.math.lalgb;

import java.nio.FloatBuffer;

import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

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

	public Matrix3f setZero()
	{
		m00 = 0f;
		m01 = 0f;
		m02 = 0f;
		m10 = 0f;
		m11 = 0f;
		m12 = 0f;
		m20 = 0f;
		m21 = 0f;
		m22 = 0f;
		return this;
	}

	public Matrix3d castToHP()
	{
		Matrix3d matrix3d = new Matrix3d();
		matrix3d.m00 = (float) m00;
		matrix3d.m01 = (float) m01;
		matrix3d.m02 = (float) m02;

		matrix3d.m10 = (float) m10;
		matrix3d.m11 = (float) m11;
		matrix3d.m12 = (float) m12;

		matrix3d.m20 = (float) m20;
		matrix3d.m21 = (float) m21;
		matrix3d.m22 = (float) m22;
		return matrix3d;
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

	public static Matrix3f mul(Matrix3f left, Matrix3f right, Matrix3f dest)
	{
		if (dest == null)
			dest = new Matrix3f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;

		return dest;
	}

	public static Vector3fm transform(Matrix3f left, Vector3fm right, Vector3fm dest)
	{
		if (dest == null)
			dest = new Vector3fm();

		float x = left.m00 * right.getX() + left.m10 * right.getY() + left.m20 * right.getZ();
		float y = left.m01 * right.getX() + left.m11 * right.getY() + left.m21 * right.getZ();
		float z = left.m02 * right.getX() + left.m12 * right.getY() + left.m22 * right.getZ();

		dest.setX(x);
		dest.setY(y);
		dest.setZ(z);

		return dest;
	}
	
	public void scale(float factor)
	{
		m00 *= factor;
		m01 *= factor;
		m02 *= factor;
		m10 *= factor;
		m11 *= factor;
		m12 *= factor;
		m20 *= factor;
		m21 *= factor;
		m22 *= factor;
	}
	
	@Override
	public String toString()
	{
		String t = "";
		String format = "%10.5f";
		t += "[ " + String.format(format, m00) + ", " + String.format(format, m10) + ", " + String.format(format, m20) + "]\n";
		t += "[ " + String.format(format, m01) + ", " + String.format(format, m11) + ", " + String.format(format, m21) + "]\n";
		t += "[ " + String.format(format, m02) + ", " + String.format(format, m12) + ", " + String.format(format, m22) + "]\n";

		return t;
	}
}
