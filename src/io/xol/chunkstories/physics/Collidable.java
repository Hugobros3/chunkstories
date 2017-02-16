package io.xol.chunkstories.physics;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Collidable
{
	public boolean collidesWith(Collidable box);
	
	public Vector3dm lineIntersection(Vector3dm lineStart, Vector3dm lineDirection);
}
