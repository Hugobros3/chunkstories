package io.xol.engine.math.lalgb;

import java.nio.FloatBuffer;

//(c) 2015-2016 XolioWare Interactive

public class Matrix4f
{
	// Hopefully hi-performance matrix class
	public float m00 = 1f;
	public float m01 = 0f;
	public float m02 = 0f;
	public float m03 = 0f;
	public float m10 = 0f;
	public float m11 = 1f;
	public float m12 = 0f;
	public float m13 = 0f;
	public float m20 = 0f;
	public float m21 = 0f;
	public float m22 = 1f;
	public float m23 = 0f;
	public float m30 = 0f;
	public float m31 = 0f;
	public float m32 = 0f;
	public float m33 = 1f;

	public Matrix4f()
	{

	}

	public void setIdentity()
	{
		Matrix4f.setIdentity(this);
	}

	public static Matrix4f setIdentity(Matrix4f matrix)
	{
		matrix.m00 = 1f;
		matrix.m01 = 0f;
		matrix.m02 = 0f;
		matrix.m03 = 0f;
		matrix.m10 = 0f;
		matrix.m11 = 1f;
		matrix.m12 = 0f;
		matrix.m13 = 0f;
		matrix.m20 = 0f;
		matrix.m21 = 0f;
		matrix.m22 = 1f;
		matrix.m23 = 0f;
		matrix.m30 = 0f;
		matrix.m31 = 0f;
		matrix.m32 = 0f;
		matrix.m33 = 1f;
		return matrix;
	}

	public Matrix4f multiply(float scalar)
	{
		m00 *= scalar;
		m01 *= scalar;
		m02 *= scalar;
		m03 *= scalar;
		m10 *= scalar;
		m11 *= scalar;
		m12 *= scalar;
		m13 *= scalar;
		m20 *= scalar;
		m21 *= scalar;
		m22 *= scalar;
		m23 *= scalar;
		m30 *= scalar;
		m31 *= scalar;
		m32 *= scalar;
		m33 *= scalar;
		return this;
	}

	public Matrix4f multiply(Matrix4f b)
	{
		m00 = m00 * b.m00 + m01 * b.m10 + m02 * b.m20 + m03 * b.m30;
		m10 = m10 * b.m00 + m11 * b.m10 + m12 * b.m20 + m13 * b.m30;
		m20 = m20 * b.m00 + m21 * b.m10 + m22 * b.m20 + m23 * b.m30;
		m30 = m30 * b.m00 + m31 * b.m10 + m32 * b.m20 + m33 * b.m30;

		m01 = m00 * b.m01 + m01 * b.m11 + m02 * b.m21 + m03 * b.m31;
		m11 = m10 * b.m01 + m11 * b.m11 + m12 * b.m21 + m13 * b.m31;
		m21 = m20 * b.m01 + m21 * b.m11 + m22 * b.m21 + m23 * b.m31;
		m31 = m30 * b.m01 + m31 * b.m11 + m32 * b.m21 + m33 * b.m31;

		m02 = m00 * b.m02 + m01 * b.m12 + m02 * b.m22 + m03 * b.m32;
		m12 = m10 * b.m02 + m11 * b.m12 + m12 * b.m22 + m13 * b.m32;
		m22 = m20 * b.m02 + m21 * b.m12 + m22 * b.m22 + m23 * b.m32;
		m32 = m30 * b.m02 + m31 * b.m12 + m32 * b.m22 + m33 * b.m32;

		m03 = m00 * b.m03 + m01 * b.m13 + m02 * b.m23 + m03 * b.m33;
		m13 = m10 * b.m03 + m11 * b.m13 + m12 * b.m23 + m13 * b.m33;
		m23 = m20 * b.m03 + m21 * b.m13 + m22 * b.m23 + m23 * b.m33;
		m33 = m30 * b.m03 + m31 * b.m13 + m32 * b.m23 + m33 * b.m33;
		return this;
	}

	/**
	 * Load from a float buffer, operable with LWJGL/OpenGL matrix4f code Stores
	 * in column major order ( m00, m01, etc )
	 * 
	 * @param buf
	 *            A float buffer of 16 size
	 */
	public Matrix4f load(FloatBuffer buf)
	{
		assert buf.capacity() == 16;
		if (!buf.hasRemaining())
		{
			if (buf.position() == buf.limit())
			{
				// System.out.println("buffer at limit, rewinding.");
				buf.rewind();
			}
			else
				return this;
		}
		m00 = buf.get();
		m01 = buf.get();
		m02 = buf.get();
		m03 = buf.get();
		m10 = buf.get();
		m11 = buf.get();
		m12 = buf.get();
		m13 = buf.get();
		m20 = buf.get();
		m21 = buf.get();
		m22 = buf.get();
		m23 = buf.get();
		m30 = buf.get();
		m31 = buf.get();
		m32 = buf.get();
		m33 = buf.get();
		return this;
	}

	public String toString()
	{
		String t = "";
		String format = "%10.5f";
		t += "[ " + String.format(format, m00) + ", " + String.format(format, m10) + ", "+ String.format(format, m20) + ", " + String.format(format, m30) + "]\n";
		t += "[ " + String.format(format, m01) + ", " + String.format(format, m11) + ", "+ String.format(format, m21) + ", " + String.format(format, m31) + "]\n";
		t += "[ " + String.format(format, m02) + ", " + String.format(format, m12) + ", "+ String.format(format, m22) + ", " + String.format(format, m32) + "]\n";
		t += "[ " + String.format(format, m03) + ", " + String.format(format, m13) + ", "+ String.format(format, m23) + ", " + String.format(format, m33) + "]";

		return t;
	}

}
