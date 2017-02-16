package io.xol.chunkstories.physics;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public final class CompoundCollidable implements Collidable
{
	public final Collidable[] elements;
	
	public CompoundCollidable(Collidable[] elements)
	{
		this.elements = elements;
	}

	@Override
	public boolean collidesWith(Collidable box)
	{
		for(Collidable c : elements)
			if(c.collidesWith(box))
				return true;
		return false;
	}

	@Override
	public Vector3dm lineIntersection(Vector3dm lineStart, Vector3dm lineDirection)
	{
		for(Collidable c : elements)
		{
			Vector3dm intersection = c.lineIntersection(lineStart, lineDirection);
			if(intersection != null)
				return intersection;
		}
		return null;
	}
}
