//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.physics;

import org.joml.Vector3d;
import org.joml.Vector3f;

public class CollisionPlane {
	public final double a, b, c, d;
	Vector3d n;

	public CollisionPlane(Vector3f p1, Vector3f p2, Vector3f p3) {
		Vector3d v = new Vector3d(p2.x(), p2.y(), p2.z());
		Vector3d u = new Vector3d(p3.x(), p3.y(), p3.z());

		Vector3d p1d = new Vector3d(p1);

		v.sub(p1d);
		u.sub(p1d);

		n = new Vector3d();

		v.cross(u, n);
		// VectorCrossProduct.cross33(v, u, n);
		n.normalize();

		a = n.x();
		b = n.y();
		c = n.z();

		d = -p1d.dot(n);
	}

	public double distance(Vector3f point) {
		return a * point.x() + b * point.y() + c * point.z() + d;
	}
}
