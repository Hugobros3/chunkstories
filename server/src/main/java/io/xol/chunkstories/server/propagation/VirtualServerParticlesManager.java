//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.propagation;

import io.xol.chunkstories.api.net.packets.PacketParticle;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;
import org.joml.Vector3dc;

public class VirtualServerParticlesManager implements ParticlesManager {
	private final WorldServer worldServer;

	public VirtualServerParticlesManager(WorldServer worldServer, DedicatedServer server) {
		this.worldServer = worldServer;
	}

	public class ServerPlayerVirtualParticlesManager implements ParticlesManager {
		ServerPlayer serverPlayer;

		public ServerPlayerVirtualParticlesManager(ServerPlayer serverPlayer) {
			this.serverPlayer = serverPlayer;
		}

		@Override public void spawnParticleAtPosition(String particleTypeName, Vector3dc position) {
			spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
		}

		@Override public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc position, Vector3dc velocity) {
			VirtualServerParticlesManager.this.spawnParticleAtPositionWithVelocity(particleTypeName, position, velocity);
		}

	}

	@Override public void spawnParticleAtPosition(String particleTypeName, Vector3dc position) {
		spawnParticleAtPositionWithVelocity(particleTypeName, position, null);
	}

	@Override public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc location, Vector3dc velocity) {
		for (Player player : worldServer.getPlayers()) {
			PacketParticle packet = new PacketParticle(worldServer, particleTypeName, location, velocity);
			player.pushPacket(packet);
		}
	}

}
