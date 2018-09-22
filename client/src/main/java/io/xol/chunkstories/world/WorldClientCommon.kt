//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world

import io.xol.chunkstories.api.graphics.systems.dispatching.DecalsManager
import io.xol.chunkstories.api.particles.ParticlesManager
import io.xol.chunkstories.api.plugin.ClientPluginManager
import io.xol.chunkstories.api.world.WorldClient
import io.xol.chunkstories.api.world.WorldInfo
import io.xol.chunkstories.client.ingame.IngameClientImplementation
import io.xol.chunkstories.content.translator.AbstractContentTranslator
import java.io.File

/**
 * Mostly the common methods of WorldClientRemote and WorldClientLocal
 */
abstract class WorldClientCommon @Throws(WorldLoadingException::class)
constructor(override val client: IngameClientImplementation, info: WorldInfo, translator: AbstractContentTranslator?, folder: File?) : WorldImplementation(client, info, translator, folder), WorldClient {

    val pluginManager: ClientPluginManager
        get() = client.pluginManager
    override val gameContext: IngameClientImplementation
        get() = client

    override val decalsManager: DecalsManager = TODO()
    override val particlesManager: ParticlesManager = TODO()

    override fun tick() {
        super.tick()

        // Update used map bits
        client.player.loadingAgent.updateUsedWorldBits()

        // Update world timing graph
        //WorldLogicTimeRenderer.tickWorld();

        // Update world effects
        //getWorldRenderer().getWorldEffectsRenderer().tick();

        // Update particles subsystem if it exists
        //if (getParticlesManager() != null && getParticlesManager() instanceof ClientParticlesRenderer)
        //	((ClientParticlesRenderer) getParticlesManager()).updatePhysics();
    }
}
