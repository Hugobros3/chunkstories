//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.propagation;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import xyz.chunkstories.api.particles.ParticleType;
import xyz.chunkstories.api.particles.ParticleTypeDefinition;
import xyz.chunkstories.api.particles.ParticlesManager;
import xyz.chunkstories.server.DedicatedServer;
import xyz.chunkstories.server.player.ServerPlayer;
import xyz.chunkstories.world.WorldServer;

public class VirtualServerParticlesManager implements ParticlesManager {
	private final WorldServer worldServer;

	public VirtualServerParticlesManager(WorldServer worldServer, DedicatedServer server) {
		this.worldServer = worldServer;
	}

	@Override public <T extends ParticleType.Particle> void spawnParticle(@NotNull String s, @NotNull Function1<? super T, Unit> function1) {

	}

	@Override public <T extends ParticleType.Particle> void spawnParticle(@NotNull ParticleTypeDefinition particleTypeDefinition, @NotNull Function1<? super T, Unit> function1) {

	}

	public class ServerPlayerVirtualParticlesManager implements ParticlesManager {
		ServerPlayer serverPlayer;

		public ServerPlayerVirtualParticlesManager(ServerPlayer serverPlayer) {
			this.serverPlayer = serverPlayer;
		}

		@Override public <T extends ParticleType.Particle> void spawnParticle(@NotNull String s, @NotNull Function1<? super T, Unit> function1) {

		}

		@Override public <T extends ParticleType.Particle> void spawnParticle(@NotNull ParticleTypeDefinition particleTypeDefinition, @NotNull Function1<? super T, Unit> function1) {

		}
	}


}
