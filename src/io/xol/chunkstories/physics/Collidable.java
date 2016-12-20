package io.xol.chunkstories.physics;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Collidable
{
	public boolean collidesWith(CollisionBox box);
	
	public boolean collidesWith(Entity entity);
	
	public Vector3dm collidesWith(Vector3dm lineStart, Vector3dm lineDirection);
}
