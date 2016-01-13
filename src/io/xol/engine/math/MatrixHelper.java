package io.xol.engine.math;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

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

	public static Matrix4f getLookAtMatrix(Vector3f position, Vector3f direction, Vector3f up)
	{
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Vector3f f = new Vector3f();
		Vector3f u = new Vector3f();
		Vector3f s = new Vector3f();
		Vector3f.sub(direction, position, f);
		f.normalise(f);
		up.normalise(u);
		Vector3f.cross(f, u, s);
		s.normalise(s);
		Vector3f.cross(s, f, u);

		matrix.m00 = s.x;
		matrix.m10 = s.y;
		matrix.m20 = s.z;
		matrix.m01 = u.x;
		matrix.m11 = u.y;
		matrix.m21 = u.z;
		matrix.m02 = -f.x;
		matrix.m12 = -f.y;
		matrix.m22 = -f.z;
		matrix.m30 = -Vector3f.dot(s, position);
		matrix.m31 = -Vector3f.dot(u, position);
		matrix.m32 = Vector3f.dot(f, position);

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
