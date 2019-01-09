//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client;

import xyz.chunkstories.api.client.Client;
import xyz.chunkstories.api.plugin.ClientPluginManager;
import xyz.chunkstories.api.plugin.ServerPluginManager;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.client.ingame.IngameClientLocalHost;
import xyz.chunkstories.plugin.DefaultPluginManager;

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
