//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.client.ingame.IngameClientLocalHost;
import io.xol.chunkstories.plugin.DefaultPluginManager;

public class ClientMasterPluginManager extends DefaultPluginManager implements ClientPluginManager, ServerPluginManager {
    IngameClientLocalHost localServerContext;

    public ClientMasterPluginManager(IngameClientLocalHost localServerContext) {
        super(localServerContext);
        this.localServerContext = localServerContext;
    }

    @Override
    public Client getClient() {
        return localServerContext;
    }

    @Override
    public Server getServer() {
        return localServerContext;
    }

}
