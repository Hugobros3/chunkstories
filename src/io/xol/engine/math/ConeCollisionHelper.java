package io.xol.engine.math;

import org.lwjgl.util.vector.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Now useless as we do proper frustrum culling, keeping it for other purposes that may arise
 * @author Gobrosse
 *
 */
public class ConeCollisionHelper
{

	public static boolean isPointInCone(float px, float py, float pz,
			float cox, float coy, float coz, float ctx, float cty, float ctz,
			float angle)
	{
		return false;
	}

	public static boolean isPointInCone(Vector3f point, Vector3f coneOrigin,
			Vector3f coneVector)
	{
		Vector3f viewerToChunk = new Vector3f();
		Vector3f.sub(point, coneOrigin, viewerToChunk);

		viewerToChunk = viewerToChunk.normalise(viewerToChunk);
		coneOrigin = coneOrigin.normalise(coneVector);

		float dot = Vector3f.dot(viewerToChunk, coneVector);
		float dotFOV = (float) Math.cos(90 * 1.6 / 180d * Math.PI);

		if (dot < dotFOV)
			return false;

		return true;
	}
}
