package io.xol.chunkstories.physics;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Collidable
{
	public boolean collidesWith(CollisionBox box);
	
	public boolean collidesWith(Entity entity);
	
	public Vector3d collidesWith(Vector3d lineStart, Vector3d lineDirection);
}
