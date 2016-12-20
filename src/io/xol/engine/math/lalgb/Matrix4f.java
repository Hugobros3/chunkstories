package io.xol.engine.math.lalgb;

import java.nio.FloatBuffer;

import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
	
	public Matrix4f(Matrix4f mat)
	{
		load(mat);
	}

	public void load(Matrix4f mat)
	{
		this.m00 = mat.m00;
		this.m01 = mat.m01;
		this.m02 = mat.m02;
		this.m03 = mat.m03;

		this.m10 = mat.m10;
		this.m11 = mat.m11;
		this.m12 = mat.m12;
		this.m13 = mat.m13;

		this.m20 = mat.m20;
		this.m21 = mat.m21;
		this.m22 = mat.m22;
		this.m23 = mat.m23;

		this.m30 = mat.m30;
		this.m31 = mat.m31;
		this.m32 = mat.m32;
		this.m33 = mat.m33;
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
	 * Load from a float buffer, operable with LWJGL/OpenGL matrix4f code Stores in column major order ( m00, m01, etc )
	 * 
	 * @param buf
	 *            A float buffer of 16 size
	 */
	public Matrix4f load(FloatBuffer buf)
	{
		assert buf.capacity() == 16;
		
		/*if (!buf.hasRemaining())
		{
			if (buf.position() == buf.limit())
			{
				// System.out.println("buffer at limit, rewinding.");
				buf.rewind();
			}
			else
				return this;
		}*/
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

	public Matrix4f store(FloatBuffer buf)
	{
		assert buf.capacity() == 16;
		buf.put(m00);
		buf.put(m01);
		buf.put(m02);
		buf.put(m03);
		buf.put(m10);
		buf.put(m11);
		buf.put(m12);
		buf.put(m13);
		buf.put(m20);
		buf.put(m21);
		buf.put(m22);
		buf.put(m23);
		buf.put(m30);
		buf.put(m31);
		buf.put(m32);
		buf.put(m33);
		return this;
	}

	@Override
	public String toString()
	{
		String t = "";
		String format = "%10.5f";
		t += "[ " + String.format(format, m00) + ", " + String.format(format, m10) + ", " + String.format(format, m20) + ", " + String.format(format, m30) + "]\n";
		t += "[ " + String.format(format, m01) + ", " + String.format(format, m11) + ", " + String.format(format, m21) + ", " + String.format(format, m31) + "]\n";
		t += "[ " + String.format(format, m02) + ", " + String.format(format, m12) + ", " + String.format(format, m22) + ", " + String.format(format, m32) + "]\n";
		t += "[ " + String.format(format, m03) + ", " + String.format(format, m13) + ", " + String.format(format, m23) + ", " + String.format(format, m33) + "]";

		return t;
	}

	public Matrix4f translate(Vector3<Float> vec3)
	{
		return translate(vec3, this, this);
	}
	
	public Matrix4f scale(Vector3<Float> vec)
	{
		return scale(vec, this, this);
	}
	
	//From LWJGL
	public static Matrix4f scale(Vector3<Float> vec, Matrix4f src, Matrix4f dest) {
		if (dest == null)
			dest = new Matrix4f();
		dest.m00 = src.m00 * vec.getX();
		dest.m01 = src.m01 * vec.getX();
		dest.m02 = src.m02 * vec.getX();
		dest.m03 = src.m03 * vec.getX();
		dest.m10 = src.m10 * vec.getY();
		dest.m11 = src.m11 * vec.getY();
		dest.m12 = src.m12 * vec.getY();
		dest.m13 = src.m13 * vec.getY();
		dest.m20 = src.m20 * vec.getZ();
		dest.m21 = src.m21 * vec.getZ();
		dest.m22 = src.m22 * vec.getZ();
		dest.m23 = src.m23 * vec.getZ();
		return dest;
	}

	//From LWJGL
	public static Matrix4f translate(Vector3<Float> vec3, Matrix4f source, Matrix4f destination)
	{
		if (destination == null)
			destination = new Matrix4f();

		destination.m30 += source.m00 * vec3.getX() + source.m10 * vec3.getY() + source.m20 * vec3.getZ();
		destination.m31 += source.m01 * vec3.getX() + source.m11 * vec3.getY() + source.m21 * vec3.getZ();
		destination.m32 += source.m02 * vec3.getX() + source.m12 * vec3.getY() + source.m22 * vec3.getZ();
		destination.m33 += source.m03 * vec3.getX() + source.m13 * vec3.getY() + source.m23 * vec3.getZ();

		return destination;
	}

	//From LWJGL
	public static Vector4fm transform(Matrix4f left, Vector4fm right, Vector4fm dest)
	{
		if (dest == null)
			dest = new Vector4fm();

		float x = left.m00 * right.getX() + left.m10 * right.getY() + left.m20 * right.getZ() + left.m30 * right.getW();
		float y = left.m01 * right.getX() + left.m11 * right.getY() + left.m21 * right.getZ() + left.m31 * right.getW();
		float z = left.m02 * right.getX() + left.m12 * right.getY() + left.m22 * right.getZ() + left.m32 * right.getW();
		float w = left.m03 * right.getX() + left.m13 * right.getY() + left.m23 * right.getZ() + left.m33 * right.getW();

		dest.setX(x);
		dest.setY(y);
		dest.setZ(z);
		dest.setW(w);

		return dest;
	}

	//From LWJGL
	public float determinant()
	{
		float f = m00 * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32) - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);
		f -= m01 * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32) - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);
		f += m02 * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31) - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33);
		f -= m03 * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31) - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);
		return f;
	}

	//From LWJGL
	private static float determinant3x3(float t00, float t01, float t02, float t10, float t11, float t12, float t20, float t21, float t22)
	{
		return t00 * (t11 * t22 - t12 * t21) + t01 * (t12 * t20 - t10 * t22) + t02 * (t10 * t21 - t11 * t20);
	}

	//From LWJGL
	public static Matrix4f invert(Matrix4f src, Matrix4f dest)
	{
		float determinant = src.determinant();

		if (determinant != 0)
		{
			/*
			 * m00 m01 m02 m03
			 * m10 m11 m12 m13
			 * m20 m21 m22 m23
			 * m30 m31 m32 m33
			 */
			if (dest == null)
				dest = new Matrix4f();
			float determinant_inv = 1f / determinant;

			// first row
			float t00 = determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t02 = determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// second row
			float t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			float t11 = determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			float t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			float t13 = determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// third row
			float t20 = determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
			float t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
			float t22 = determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
			float t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
			// fourth row
			float t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
			float t31 = determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
			float t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
			float t33 = determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

			// transpose and divide by the determinant
			dest.m00 = t00 * determinant_inv;
			dest.m11 = t11 * determinant_inv;
			dest.m22 = t22 * determinant_inv;
			dest.m33 = t33 * determinant_inv;
			dest.m01 = t10 * determinant_inv;
			dest.m10 = t01 * determinant_inv;
			dest.m20 = t02 * determinant_inv;
			dest.m02 = t20 * determinant_inv;
			dest.m12 = t21 * determinant_inv;
			dest.m21 = t12 * determinant_inv;
			dest.m03 = t30 * determinant_inv;
			dest.m30 = t03 * determinant_inv;
			dest.m13 = t31 * determinant_inv;
			dest.m31 = t13 * determinant_inv;
			dest.m32 = t23 * determinant_inv;
			dest.m23 = t32 * determinant_inv;
			return dest;
		}
		else
			return null;
	}

	public static Matrix4f transpose(Matrix4f src, Matrix4f dest)
	{
		if (dest == null)
			dest = new Matrix4f();
		float m00 = src.m00;
		float m01 = src.m10;
		float m02 = src.m20;
		float m03 = src.m30;
		float m10 = src.m01;
		float m11 = src.m11;
		float m12 = src.m21;
		float m13 = src.m31;
		float m20 = src.m02;
		float m21 = src.m12;
		float m22 = src.m22;
		float m23 = src.m32;
		float m30 = src.m03;
		float m31 = src.m13;
		float m32 = src.m23;
		float m33 = src.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}

	public Matrix4f rotate(float angle, Vector3fm axis)
	{
		return rotate(angle, axis, this, this);
	}

	//From LWJGL
	public static Matrix4f rotate(float angle, Vector3fm axis, Matrix4f src, Matrix4f dest)
	{
		if (dest == null)
			dest = new Matrix4f();
		float c = (float) Math.cos(angle);
		float s = (float) Math.sin(angle);
		float oneminusc = 1.0f - c;
		float xy = axis.getX() * axis.getY();
		float yz = axis.getY() * axis.getZ();
		float xz = axis.getX() * axis.getZ();
		float xs = axis.getX() * s;
		float ys = axis.getY() * s;
		float zs = axis.getZ() * s;

		float f00 = axis.getX() * axis.getX() * oneminusc + c;
		float f01 = xy * oneminusc + zs;
		float f02 = xz * oneminusc - ys;
		// n[3] not used
		float f10 = xy * oneminusc - zs;
		float f11 = axis.getY() * axis.getY() * oneminusc + c;
		float f12 = yz * oneminusc + xs;
		// n[7] not used
		float f20 = xz * oneminusc + ys;
		float f21 = yz * oneminusc - xs;
		float f22 = axis.getZ() * axis.getZ() * oneminusc + c;

		float t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
		float t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
		float t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
		float t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
		float t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
		float t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
		float t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
		float t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
		dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
		dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
		dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
		dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
		dest.m00 = t00;
		dest.m01 = t01;
		dest.m02 = t02;
		dest.m03 = t03;
		dest.m10 = t10;
		dest.m11 = t11;
		dest.m12 = t12;
		dest.m13 = t13;
		return dest;
	}

	public static Matrix4f mul(Matrix4f left, Matrix4f right, Matrix4f dest)
	{
		if (dest == null)
			dest = new Matrix4f();

		float m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
		float m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
		float m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
		float m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
		float m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
		float m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
		float m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
		float m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
		float m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
		float m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
		float m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
		float m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
		float m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
		float m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
		float m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
		float m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

		dest.m00 = m00;
		dest.m01 = m01;
		dest.m02 = m02;
		dest.m03 = m03;
		dest.m10 = m10;
		dest.m11 = m11;
		dest.m12 = m12;
		dest.m13 = m13;
		dest.m20 = m20;
		dest.m21 = m21;
		dest.m22 = m22;
		dest.m23 = m23;
		dest.m30 = m30;
		dest.m31 = m31;
		dest.m32 = m32;
		dest.m33 = m33;

		return dest;
	}
	
	public Matrix4f clone()
	{
		return new Matrix4f(this);
	}
}
