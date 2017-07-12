package io.xol.chunkstories.physics;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.physics.Collidable;

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
	public Vector3dc lineIntersection(Vector3dc lineStart, Vector3dc lineDirection)
	{
		for(Collidable c : elements)
		{
			Vector3dc intersection = c.lineIntersection(lineStart, lineDirection);
			if(intersection != null)
				return intersection;
		}
		return null;
	}
}
