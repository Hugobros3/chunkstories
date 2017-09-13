package io.xol.chunkstories.server.propagation;

import java.util.Iterator;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.net.packets.PacketParticle;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.world.WorldServer;
import io.xol.chunkstories.server.ServerPlayer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VirtualServerParticlesManager implements ParticlesManager
{
	WorldServer worldServer;
	
	public VirtualServerParticlesManager(WorldServer worldServer, DedicatedServer server)
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
		public void spawnParticleAtPosition(String particleTypeName, Vector3dc position)
		{
			spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc position, Vector3dc velocity)
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
	
	void tellPlayer(Player player, String particleTypeName, Vector3dc location, Vector3dc velocity)
	{
		PacketParticle packet = new PacketParticle(particleTypeName, location, velocity);
		/*packet.particleName = particleTypeName;
		packet.position = location;
		packet.velocity = velocity;*/
		player.pushPacket(packet);
	}

	@Override
	public void spawnParticleAtPosition(String particleTypeName, Vector3dc position)
	{
		spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
	}

	@Override
	public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc location, Vector3dc velocity)
	{
		Iterator<Player> i = worldServer.getPlayers();
		while(i.hasNext())
		{
			Player player = i.next();
			tellPlayer(player, particleTypeName, location, velocity);
		}
	}

}
