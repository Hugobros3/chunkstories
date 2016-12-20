package io.xol.chunkstories.api.particles;

import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Particle data is at least a vector3f
 */
public class ParticleData extends Vector3fm
{
	boolean ded = false;
	
	public ParticleData(float x, float y, float z)
	{
		super(x, y, z);
	}
	
	public void destroy()
	{
		ded = true;
	}
	
	public boolean isDed()
	{
		return ded;
	}
}
