package io.xol.chunkstories.core.util;

import io.xol.chunkstories.api.Location;
import org.joml.Vector3d;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.net.packets.PacketExplosionEffect;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Creates an explosion with particles and sounds */
public class WorldEffects
{
	public static void createFireball(World world, Vector3d center, double radius, double debrisSpeed, float f)
	{
		//Play effect directly in SP
		if(world instanceof WorldClient && (world instanceof WorldMaster))
			createFireballFx(world, center, radius, debrisSpeed, f);
		//Dispatch to users in MP
		else if(world instanceof WorldMaster)
		{
			PacketExplosionEffect packet = new PacketExplosionEffect(center, radius, debrisSpeed, f);
			WorldMaster ws = (WorldMaster)world;
			for(Player p : ws.getPlayers())
			{
				if(p.hasSpawned())
				{
					Location ploc = p.getLocation();
					if(ploc == null)
						continue;
					if(ploc.distance(center) > 1024)
						continue;
					p.pushPacket(packet);
				}
			}
		}
		else {
			//TODO WorldClientServer
		}
		
		//Play the sound more directly
		world.getSoundManager().playSoundEffect("./sounds/sfx/kboom.ogg", center, (float)(0.9f + Math.random() * 0.2f), (float)(debrisSpeed * debrisSpeed * 10f), 1, 150);
	}
	
	public static void createFireballFx(World world, Vector3d center, double radius, double debrisSpeed, float f)
	{
		for(int z = 0; z < 250 * f; z++)
		{
			Vector3d lol = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
			lol.normalize();
			
			Vector3d spd = new Vector3d(lol);
			spd.mul(debrisSpeed * (0.5 + Math.random()));
			
			lol.mul(radius);
			lol.add(center);
			
			world.getParticlesManager().spawnParticleAtPositionWithVelocity("fire", lol, spd);
		}
		world.getParticlesManager().spawnParticleAtPositionWithVelocity("fire_light", center, new Vector3d(1, 0, 0).normalize().mul(debrisSpeed * 1.5f));
	}
}
