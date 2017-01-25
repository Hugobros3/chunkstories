package io.xol.chunkstories.physics;

import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.operations.VectorCrossProduct;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

public class CollisionPlane
{
	public final double a, b, c, d;
	Vector3dm n;
	
	public CollisionPlane(Vector3fm p1, Vector3fm p2, Vector3fm p3)
	{
		Vector3dm v = new Vector3dm(p2.getX(), p2.getY(), p2.getZ());
		Vector3dm u = new Vector3dm(p3.getX(), p3.getY(), p3.getZ());
		
		v.sub(p1.castToDoublePrecision());
		u.sub(p1.castToDoublePrecision());
		
		n = new Vector3dm();
		VectorCrossProduct.cross33(v, u, n);
		n.normalize();
		
		a = n.getX();
		b = n.getY();
		c = n.getZ();
		
		d = -p1.castToDoublePrecision().dot(n);
	}
	
	public double distance(Vector3fm point)
	{
		return a * point.getX() + b * point.getY() + c * point.getZ() + d;
	}
}
