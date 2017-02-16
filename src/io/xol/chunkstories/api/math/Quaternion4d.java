package io.xol.chunkstories.api.math;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.operations.VectorCrossProduct;

/* This file is part of the ChunkStories WIP API.
 * Licensing is yet to be decided, please check out the github page for
 * up-to date information. */

/** Hopefully hi-performance matrix class, replacing and using bits from LWJGL2 utility library */
public class Quaternion4d
{
	public double s;
	public Vector3dm v;
	
	public Quaternion4d()
	{
		s = 0.0;
		v = new Vector3dm(0.0);
	}
	
	public Quaternion4d(double s, Vector3dm v)
	{
		this.s = s;
		this.v = v.clone();
	}
	
	public Quaternion4d(Quaternion4d quat)
	{
		this.s = quat.s;
		this.v = new Vector3dm(quat.v);
	}
	
	public Quaternion4d add(Quaternion4d quat)
	{
		s += quat.s;
		v.add(quat.v);
		
		return this;
	}
	
	public Quaternion4d sub(Quaternion4d quat)
	{
		s -= quat.s;
		v.sub(quat.v);
		
		return this;
	}
	
	public Quaternion4d scale(double scalar)
	{
		s *= scalar;
		v.scale(scalar);
		return this;
	}
	
	public Quaternion4d mult(Quaternion4d quat)
	{
		return mult(this, quat, null);
	}
	
	public static Quaternion4d mult(Quaternion4d a, Quaternion4d b, Quaternion4d out)
	{
		if(out == null)
			out = new Quaternion4d();
		
		// [ Sa.Sb - a.b ,
		out.s = a.s * b.s - a.v.dot(b.v);
		// Sa.b + Sb.a + a x b ]
		Vector3dm aBv = b.v.clone().scale(a.s);
		Vector3dm vBa = a.v.clone().scale(b.s);
		
		out.v = aBv.add(vBa).add(VectorCrossProduct.cross33(a.v, b.v));
		//out.v = Vector3dm.add(aBv, vBa, null).add(Vector3dm.cross(a.v, b.v));
		
		return out;
	}
	
	public Quaternion4d conjugate()
	{
		return new Quaternion4d(s, v.clone().negate());
	}
	
	public double norm()
	{
		return Math.sqrt(s * s + v.dot(v) * v.dot(v));
		//return Math.sqrt(s * s + Vector3dm.dot(v, v) * Vector3dm.dot(v, v));
	}
	
	public Quaternion4d normalize()
	{
		this.scale(1.0 / (norm()));
		
		return this;
	}
	
	public Quaternion4d inverse()
	{
		Quaternion4d inverse = conjugate();
		inverse.scale( 1.0 / (norm() * norm()));
		return inverse;
	}
	
	public static double dot(Quaternion4d a, Quaternion4d b)
	{
		return a.s * b.s + a.v.getX() * b.v.getX() + a.v.getY() * b.v.getY() + a.v.getZ() * b.v.getZ();
	}
	
	public static Quaternion4d fromAxisAngle(Vector3dm axis, double angle)
	{
		angle /= 2.0;
		return new Quaternion4d(Math.cos(angle), axis.clone().scale(Math.sin(angle)));
	}
	
	public static Vector3dm rotate(Vector3dm vector, Vector3dm axis, double angle)
	{
		//Make quaternion out of the vector
		Quaternion4d p = new Quaternion4d(0.0, vector);
		
		//Make a rotation quaternion about the vector
		Quaternion4d q = fromAxisAngle(axis, angle);
		
		//Apply rotation
		Quaternion4d iq = q.inverse();
		// qpq^-1
		return ((q.mult(p)).mult(iq)).v;
	}
	
	public static Quaternion4d slerp(Quaternion4d q1, Quaternion4d q2, double t)
	{
		//q1.normalize();
		//q2.normalize();
		
		double dot = Quaternion4d.dot(q1, q2);
		
		//Ignore cases where the two Quaternion4d are identical
		if(dot <= -1.0 || dot >= 1.0)
		{
			return q1;
		}
		
		//No interp
		if(t <= 0.0)
			return q1;
		
		//No interp
		if(t >= 1.0)
			return q2;
		
		//Avoid taking the long path
		if(dot < 0.0)
		{
			dot *= -1.0;
			q1.scale(-1.0);
		}
		
		//Get the angle
		double omega = Math.acos(dot);
		
		//Compute numerators and denominators
		double sinOmega = Math.sin(omega);
		double sinOmegaT = Math.sin(omega * t);
		double sinOmega1_T = Math.sin(omega * (1.0 - t));
		
		//Security : 
		if(sinOmega == 0.0)
		{
			return q1;
		}
		
		//Scale vectors to fractions
		Quaternion4d left = new Quaternion4d(q1);
		left.scale(sinOmega1_T / sinOmega);

		Quaternion4d right = new Quaternion4d(q2);
		right.scale(sinOmegaT / sinOmega);
		
		//Add vectors
		Quaternion4d result = new Quaternion4d();
		result.add(left);
		result.add(right);
		return result;
	}
	
	public Matrix4f toMatrix4f()
	{
		Matrix4f matrix = new Matrix4f();

		double n = v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ() + s * s;
		double ss = (n > 0.0f) ? (2.0f / n) : 0.0f;
        
		double xs = v.getX() * ss, ys = v.getY() * ss, zs = v.getZ() * ss;
		double wx = s * xs, wy = s * ys, wz = s * zs;
		double xx = v.getX() * xs, xy = v.getX() * ys, xz = v.getX() * zs;
		double yy = v.getY() * ys, yz = v.getY() * zs, zz = v.getZ() * zs;
        
        matrix.m00 = (float) ( 1.0f - (yy + zz ) );
        matrix.m10 = (float) ( xy - wz );
        matrix.m20 = (float) ( xz + wy );
        matrix.m01 = (float) ( xy + wz );
        matrix.m11 = (float) ( 1.0f - ( xx + zz ) );
        matrix.m21 = (float) ( yz - wx );
        matrix.m02 = (float) ( xz - wy );
        matrix.m12 = (float) ( yz + wx );
        matrix.m22 = (float) ( 1.0f - ( xx + yy ) );
        
        return matrix;
	}
	
	public String toString()
	{
		return "[Quaternion4d s:"+s+" v:"+v+"]";
	}
	
	public Quaternion4d clone()
	{
		return new Quaternion4d(this);
	}
}
