package io.xol.engine.math;

import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.operations.VectorCrossProduct;
import io.xol.chunkstories.api.math.vector.sp.Vector3fm;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Various helper functions to build matrices
 */
public class MatrixHelper
{
	public static Matrix4f getOrthographicMatrix(float left, float right, float bottom, float top, float near, float far)
	{
		Matrix4f matrix = new Matrix4f();
		float width = right - left;
		float height = top - bottom;
		float depth = far - near;

		matrix.m00 = 2.0f / width;
		matrix.m11 = 2.0f / height;
		matrix.m22 = -2.0f / depth;
		matrix.m30 = -(right + left) / (right - left);
		matrix.m31 = -(top + bottom) / (top - bottom);
		matrix.m32 = -(far + near) / (far - near);
		return matrix;
	}

	public static Matrix4f getLookAtMatrix(Vector3<?> position, Vector3<Float> direction, Vector3<Float> up)
	{
		if(direction.getY() == 1.0f || direction.getY() == -1.0f)
			up = new Vector3fm(1.0f, 0.0f, 0.0f);
		
		Vector3<Float> positionSP = position.castToSinglePrecision();
		//System.out.println(positionSP);
		
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Vector3fm f = new Vector3fm(direction);
		Vector3fm u = new Vector3fm();
		Vector3fm s = new Vector3fm();
		
		f.sub(positionSP);
		//Vector3fm.sub(direction, position, f);
		f.normalize();
		VectorCrossProduct.cross33(f, up, s);
		s.normalize();
		VectorCrossProduct.cross33(s, f, u);

		//System.out.println(u);
		
		matrix.m00 = s.getX();
		matrix.m10 = s.getY();
		matrix.m20 = s.getZ();
		matrix.m01 = u.getX();
		matrix.m11 = u.getY();
		matrix.m21 = u.getZ();
		matrix.m02 = -f.getX();
		matrix.m12 = -f.getY();
		matrix.m22 = -f.getZ();
		matrix.m30 = -s.dot(positionSP);
		matrix.m31 = -u.dot(positionSP);
		matrix.m32 = f.dot(positionSP);
		
		return matrix;
	}

	public static Matrix4f getBiasMatrix()
	{
		Matrix4f matrix = new Matrix4f();

		matrix.m00 = 0.5f;
		matrix.m11 = 0.5f;
		matrix.m22 = 0.5f;
		matrix.m30 = 0.5f;
		matrix.m31 = 0.5f;
		matrix.m32 = 0.5f;
		return matrix;
	}
}
