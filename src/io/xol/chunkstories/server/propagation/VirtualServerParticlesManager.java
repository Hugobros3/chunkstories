package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.net.packets.PacketParticle;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.RemoteServerPlayer;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VirtualServerParticlesManager implements ParticlesManager
{
	WorldServer worldServer;
	
	public VirtualServerParticlesManager(WorldServer worldServer, Server server)
	{
		this.worldServer = worldServer;
	}
	
	public class ServerPlayerVirtualParticlesManager implements ParticlesManager
	{
		RemoteServerPlayer serverPlayer;

		public ServerPlayerVirtualParticlesManager(RemoteServerPlayer serverPlayer)
		{
			this.serverPlayer = serverPlayer;
		}

		@Override
		public void spawnParticleAtPosition(String particleTypeName, Vector3dm position)
		{
			spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dm position, Vector3dm velocity)
		{
			Iterator<Player> i = worldServer.getPlayers();
			while(i.hasNext())
			{
				Player player = i.next();
				if(!player.equals(serverPlayer))
					tellPlayer(player, particleTypeName, position, velocity);
			}
		}

	}
	
	void tellPlayer(Player player, String particleTypeName, Vector3dm location, Vector3dm velocity)
	{
		PacketParticle packet = new PacketParticle();
		packet.particleName = particleTypeName;
		packet.position = location;
		packet.velocity = velocity;
		player.pushPacket(packet);
	}

	@Override
	public void spawnParticleAtPosition(String particleTypeName, Vector3dm position)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
	}

	@Override
	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dm location, Vector3dm velocity)
	{
		Iterator<Player> i = worldServer.getPlayers();
		while(i.hasNext())
		{
			Player player = i.next();
			tellPlayer(player, particleTypeName, location, velocity);
		}
	}

}
