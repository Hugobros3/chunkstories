package io.xol.chunkstories.api.particles;

import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ParticlesManager
{
	public void spawnParticleAtPosition(String particleTypeName, Vector3d position);
	
	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3d position, Vector3d velocity);
}
