package io.xol.chunkstories.core.util;

import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Creates an explosion with particles and sounds */
public class WorldEffects
{
	public static void createFireball(World world, Vector3dm center, double radius, double debrisSpeed, float f)
	{
		for(int z = 0; z < 250 * f; z++)
		{
			Vector3dm lol = new Vector3dm(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
			lol.normalize();
			
			Vector3dm spd = lol.clone();
			spd.scale(debrisSpeed * (0.5 + Math.random()));
			
			lol.scale(radius);
			lol.add(center);
			
			world.getParticlesManager().spawnParticleAtPositionWithVelocity("fire", lol, spd);
		}
		
		world.getParticlesManager().spawnParticleAtPositionWithVelocity("fire_light", center, new Vector3dm(1, 0, 0).normalize().scale(debrisSpeed * 1.5f));
		
		world.getSoundManager().playSoundEffect("./sounds/sfx/kboom.ogg", center, (float)(0.9f + Math.random() * 0.2f), (float)(debrisSpeed * debrisSpeed * 10f), 1, 150);
	}
}
