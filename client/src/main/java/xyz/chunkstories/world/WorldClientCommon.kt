//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

import xyz.chunkstories.api.graphics.systems.dispatching.DecalsManager
import xyz.chunkstories.api.particles.ParticlesManager
import xyz.chunkstories.api.plugin.PluginManager
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.content.translator.AbstractContentTranslator
import java.io.File

/**
 * Mostly the common methods of WorldClientRemote and WorldClientLocal
 */
abstract class WorldClientCommon @Throws(WorldLoadingException::class)
constructor(override val client: IngameClientImplementation, info: WorldInfo, translator: AbstractContentTranslator?, folder: File?) : WorldImplementation(client, info, translator, folder), WorldClient {

    val pluginManager: PluginManager
        get() = client.pluginManager

    /** We cast and return the super actualProperty because of callbacks in the superconstructor expect this to be set before we have a chance to in this constructor */
    override val gameContext: IngameClientImplementation
        get() = super.gameContext as IngameClientImplementation

    //TODO
    override val decalsManager: DecalsManager = WorldTool.NullDecalsManager()
    override val particlesManager: ParticlesManager = WorldTool.NullParticlesManager()

    override fun tick() {
        super.tick()

        client.player.update()

        // Update world timing graph
        //WorldLogicTimeRenderer.tickWorld();

        // Update world effects
        //getWorldRenderer().getWorldEffectsRenderer().tick();

        // Update particles subsystem if it exists
        //if (getParticlesManager() != null && getParticlesManager() instanceof ClientParticlesRenderer)
        //	((ClientParticlesRenderer) getParticlesManager()).updatePhysics();
    }
}
