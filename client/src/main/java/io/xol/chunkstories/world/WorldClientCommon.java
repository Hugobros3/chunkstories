//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import io.xol.chunkstories.api.content.ContentTranslator;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.ingame.IngameClientImplementation;

import java.io.File;

/**
 * Mostly the common methods of WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient {

    protected final IngameClientImplementation client;

    public WorldClientCommon(IngameClientImplementation client, WorldInfo info, ContentTranslator translator, File folder)
            throws WorldLoadingException {
        super(client, info, translator, folder);
        this.client = client;
    }

    public ClientPluginManager getPluginManager() {
        return client.getPluginManager();
    }

    @Override
    public IngameClientImplementation getClient() {
        return client;
    }

    public IngameClientImplementation getGameContext() {
        return getClient();
    }


    @Override
    public void tick() {
        super.tick();

        // Update used map bits
        getClient().getPlayer().loadingAgent.updateUsedWorldBits();

        // Update world timing graph
        //WorldLogicTimeRenderer.tickWorld();

        // Update world effects
        //getWorldRenderer().getWorldEffectsRenderer().tick();

        // Update particles subsystem if it exists
        //if (getParticlesManager() != null && getParticlesManager() instanceof ClientParticlesRenderer)
        //	((ClientParticlesRenderer) getParticlesManager()).updatePhysics();
    }
}
