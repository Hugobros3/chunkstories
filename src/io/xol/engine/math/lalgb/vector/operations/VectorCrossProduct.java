package io.xol.engine.math.lalgb.vector.operations;

import io.xol.engine.math.lalgb.Vector3f;

public class VectorCrossProduct
{
	public static Vector3f cross44(Vector3f left, Vector3f right)
	{
		return cross33(left, right, null);
	}
	
	public static Vector3f cross33(Vector3f left, Vector3f right, Vector3f dest)
	{
		if (dest == null)
			dest = new Vector3f();
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		dest.set(left.getY() * right.getZ() - left.getZ() * right.getY(), right.getX() * left.getZ() - right.getZ() * left.getX(), left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}
}
