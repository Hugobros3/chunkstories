package io.xol.engine.math.lalgb.vector.operations;

import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector3m;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

public class VectorCrossProduct
{
	
	public static Vector3dm cross33(Vector3dm left, Vector3dm right)
	{
		return cross33(left, right, null);
	}
	
	public static Vector3dm cross33(Vector3dm left, Vector3dm right, Vector3dm dest)
	{
		if (dest == null)
			dest = new Vector3dm();
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		dest.set(left.getY() * right.getZ() - left.getZ() * right.getY(), right.getX() * left.getZ() - right.getZ() * left.getX(), left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}
	
	public static Vector3m<Float> cross33(Vector3<Float> left, Vector3<Float> right)
	{
		return cross33(left, right, null);
	}
	
	public static Vector3m<Float> cross33(Vector3<Float> left, Vector3<Float> right, Vector3m<Float> dest)
	{
		if (dest == null)
			dest = new Vector3fm();
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		dest.set(left.getY() * right.getZ() - left.getZ() * right.getY(), right.getX() * left.getZ() - right.getZ() * left.getX(), left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}
	
	public static Vector3m<Double> cross33Double(Vector3<Double> left, Vector3<Double> right)
	{
		return cross33Double(left, right, null);
	}
	
	public static Vector3m<Double> cross33Double(Vector3<Double> left, Vector3<Double> right, Vector3m<Double> dest)
	{
		if (dest == null)
			dest = new Vector3dm();
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);
		//dest.set(left.y * right.z - left.z * right.y, right.x * left.z - right.z * left.x, left.x * right.y - left.y * right.x);

		dest.set(left.getY() * right.getZ() - left.getZ() * right.getY(), right.getX() * left.getZ() - right.getZ() * left.getX(), left.getX() * right.getY() - left.getY() * right.getX());
		return dest;
	}
}
