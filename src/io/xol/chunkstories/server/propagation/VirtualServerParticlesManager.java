package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.net.packets.PacketParticle;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
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
		ServerPlayer serverPlayer;

		public ServerPlayerVirtualParticlesManager(ServerPlayer serverPlayer)
		{
			this.serverPlayer = serverPlayer;
		}

		@Override
		public void spawnParticleAtPosition(String particleTypeName, Vector3d position)
		{
			spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3d position, Vector3d velocity)
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
	
	void tellPlayer(Player player, String particleTypeName, Vector3d location, Vector3d velocity)
	{
		PacketParticle packet = new PacketParticle();
		packet.particleName = particleTypeName;
		packet.position = location;
		packet.velocity = velocity;
		player.pushPacket(packet);
	}

	@Override
	public void spawnParticleAtPosition(String particleTypeName, Vector3d position)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
	}

	@Override
	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3d location, Vector3d velocity)
	{
		Iterator<Player> i = worldServer.getPlayers();
		while(i.hasNext())
		{
			Player player = i.next();
			tellPlayer(player, particleTypeName, location, velocity);
		}
	}

}
