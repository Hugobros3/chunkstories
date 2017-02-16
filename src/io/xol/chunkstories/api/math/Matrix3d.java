package io.xol.chunkstories.api.math;

import java.nio.FloatBuffer;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Matrix3d
{
	// Hopefully hi-performance matrix class
	public double m00 = 1f;
	public double m01 = 0f;
	public double m02 = 0f;
	
	public double m10 = 0f;
	public double m11 = 1f;
	public double m12 = 0f;
	
	public double m20 = 0f;
	public double m21 = 0f;
	public double m22 = 1f;

	public Matrix3d()
	{

	}
	
	public Matrix3d(
			double m00, double m01, double m02, 
			double m10, double m11, double m12, 
			double m20, double m21, double m22)
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;

		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;

		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}
	
	public Matrix3d(Matrix3d cloneme)
	{
		this.m00 = cloneme.m00;
		this.m01 = cloneme.m01;
		this.m02 = cloneme.m02;

		this.m10 = cloneme.m10;
		this.m11 = cloneme.m11;
		this.m12 = cloneme.m12;

		this.m20 = cloneme.m20;
		this.m21 = cloneme.m21;
		this.m22 = cloneme.m22;
	}
	
	public Matrix3d(Vector3dm rightDirection, Vector3dm upDirection, Vector3dm frontDirection)
	{
		//1,0,0 * M = front
		//0,1,0 * M = up
		//0,0,1 * M = right
		
		//front.x = 1 * m00
		//front.y = 1 * m10
		//front.z = 1 * m20
		
		//up.x = 1 * m01 ...
		
		m00 = rightDirection.getX();
		m10 = rightDirection.getY();
		m20 = rightDirection.getZ();
		
		m01 = upDirection.getX();
		m11 = upDirection.getY();
		m21 = upDirection.getZ();
		
		m02 = frontDirection.getX();
		m12 = frontDirection.getY();
		m22 = frontDirection.getZ();
	}
	
	public Matrix3d(Matrix3f castme)
	{
		this(castme.castToHP());
	}

	public Matrix3d setIdentity()
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

	public Matrix3d setZero()
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

	public Matrix3d store(FloatBuffer buf)
	{
		this.castToSP().store(buf);
		return this;
	}

	public Matrix3f castToSP()
	{
		Matrix3f matrix3f = new Matrix3f();
		matrix3f.m00 = (float)m00;
		matrix3f.m01 = (float)m01;
		matrix3f.m02 = (float)m02;

		matrix3f.m10 = (float)m10;
		matrix3f.m11 = (float)m11;
		matrix3f.m12 = (float)m12;

		matrix3f.m20 = (float)m20;
		matrix3f.m21 = (float)m21;
		matrix3f.m22 = (float)m22;
		return matrix3f;
	}

	//From LWJGL
	public static Matrix3d invert(Matrix3d src, Matrix3d dest)
	{
		double determinant = src.determinant();

		if (determinant != 0)
		{
			if (dest == null)
				dest = new Matrix3d();
			
			double determinant_inv = 1f / determinant;

			// get the conjugate matrix
			double t00 = src.m11 * src.m22 - src.m12 * src.m21;
			double t01 = -src.m10 * src.m22 + src.m12 * src.m20;
			double t02 = src.m10 * src.m21 - src.m11 * src.m20;
			double t10 = -src.m01 * src.m22 + src.m02 * src.m21;
			double t11 = src.m00 * src.m22 - src.m02 * src.m20;
			double t12 = -src.m00 * src.m21 + src.m01 * src.m20;
			double t20 = src.m01 * src.m12 - src.m02 * src.m11;
			double t21 = -src.m00 * src.m12 + src.m02 * src.m10;
			double t22 = src.m00 * src.m11 - src.m01 * src.m10;

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

	public double determinant()
	{
		double f = m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
		return f;
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
	
	public static Matrix3d mul(Matrix3d left, Matrix3d right, Matrix3d dest)
	{
		if (dest == null)
			dest = new Matrix3d();
		
		double m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02;
		double m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02;
		double m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02;
		double m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12;
		double m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12;
		double m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12;
		double m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22;
		double m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22;
		double m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22;
		
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

	public static Vector3dm transform(Matrix3d left, Vector3dm right, Vector3dm dest)
	{
		if (dest == null)
			dest = new Vector3dm();

		double x = left.m00 * right.getX() + left.m10 * right.getY() + left.m20 * right.getZ();
		double y = left.m01 * right.getX() + left.m11 * right.getY() + left.m21 * right.getZ();
		double z = left.m02 * right.getX() + left.m12 * right.getY() + left.m22 * right.getZ();

		dest.setX(x);
		dest.setY(y);
		dest.setZ(z);

		return dest;
	}
	
	public static Vector3dm transform(Vector3dm left, Matrix3d right, Vector3dm dest)
	{
		if (dest == null)
			dest = new Vector3dm();

		double x = left.getX() * right.m00 + left.getY() * right.m01 + left.getZ() * right.m02;
		double y = left.getX() * right.m10 + left.getY() * right.m11 + left.getZ() * right.m12;
		double z = left.getX() * right.m20 + left.getY() * right.m21 + left.getZ() * right.m22;

		dest.setX(x);
		dest.setY(y);
		dest.setZ(z);
		return dest;
	}
	
	public Matrix3d transpose()
	{
		Matrix3d matrix = new Matrix3d();
		matrix.m00 = m00;
		matrix.m11 = m11;
		matrix.m22 = m22;
	
		matrix.m01 = m10;
		matrix.m02 = m20;

		matrix.m10 = m10;
		matrix.m12 = m21;
		
		matrix.m20 = m02;
		matrix.m21 = m12;
		
		return matrix;
	}

	public static Matrix3d add(Matrix3d left, Matrix3d right, Matrix3d dest)
	{
		if (dest == null)
			dest = new Matrix3d();
		
		dest.m00 = left.m00 + right.m00;
		dest.m01 = left.m01 + right.m01;
		dest.m02 = left.m02 + right.m02;

		dest.m10 = left.m10 + right.m10;
		dest.m11 = left.m11 + right.m11;
		dest.m12 = left.m12 + right.m12;

		dest.m20 = left.m20 + right.m20;
		dest.m21 = left.m21 + right.m21;
		dest.m22 = left.m22 + right.m22;
		
		return dest;
	}

	public void scale(double factor)
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
}
