//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.propagation

import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.player.ServerPlayer
import xyz.chunkstories.world.WorldMasterImplementation

class VirtualServerParticlesManager(private val world: WorldMasterImplementation, server: DedicatedServer?) : ParticlesManager {
    override fun <T : ParticleType.Particle> spawnParticle(typeName: String, init: T.() -> Unit) {}
    override fun <T : ParticleType.Particle> spawnParticle(type: ParticleTypeDefinition, init: T.() -> Unit) {}

    inner class ServerPlayerVirtualParticlesManager(var serverPlayer: ServerPlayer) : ParticlesManager {
        override fun <T : ParticleType.Particle> spawnParticle(typeName: String, init: T.() -> Unit) {}
        override fun <T : ParticleType.Particle> spawnParticle(type: ParticleTypeDefinition, init: T.() -> Unit) {}
    }
}