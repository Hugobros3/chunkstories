//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import io.xol.chunkstories.api.content.ContentTranslator;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.ClientSlavePluginManager;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.renderer.debug.WorldLogicTimeRenderer;
import io.xol.chunkstories.renderer.decals.DecalsRendererImplementation;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.server.LocalServerContext;
import io.xol.chunkstories.server.commands.InstallServerCommands;
import io.xol.chunkstories.server.commands.content.ReloadContentCommand;

/**
 * Mostly the common methods of WorldClientRemote and WorldClientLocal
 */
public abstract class WorldClientCommon extends WorldImplementation implements WorldClient {
	protected WorldRendererImplementation renderer;

	private  LocalServerContext localServer;
	//private ClientPluginManager pluginManager;
	
	public WorldClientCommon(Client client, WorldInfoImplementation info) throws WorldLoadingException {
		this(client, info, null);
	}

	public WorldClientCommon(Client client, WorldInfoImplementation info, ContentTranslator translator)
			throws WorldLoadingException {
		super(client, info, translator);

		ClientPluginManager pluginManager;
		
		//Start a mini server
		if(this instanceof WorldMaster)
		{
			localServer = new LocalServerContext(Client.getInstance());
			pluginManager = localServer.getPluginManager();

			pluginManager.reloadPlugins();
			client.setClientPluginManager(pluginManager);
			
			new InstallServerCommands(localServer);
			new ReloadContentCommand(Client.getInstance());
		}
		else
		{
			localServer = null;
			pluginManager = new ClientSlavePluginManager(Client.getInstance());
			new ReloadContentCommand(Client.getInstance());
		}
		
		this.renderer = new WorldRendererImplementation(this, client);
	}

	public ClientPluginManager getPluginManager() {
		return Client.getInstance().getPluginManager();
	}

	@Override
	public Client getClient() {
		return Client.getInstance();
	}

	public Client getGameContext() {
		return getClient();
	}

	@Override
	public WorldRendererImplementation getWorldRenderer() {
		return renderer;
	}

	@Override
	public DecalsRendererImplementation getDecalsManager() {
		return renderer.getDecalsRenderer();
	}

	@Override
	public ClientParticlesRenderer getParticlesManager() {
		return renderer.getParticlesRenderer();
	}

	@Override
	public void tick() {
		super.tick();

		// Update used map bits
		getClient().getPlayer().loadingAgent.updateUsedWorldBits();

		// Update world timing graph
		WorldLogicTimeRenderer.tickWorld();

		// Update world effects
		getWorldRenderer().getWorldEffectsRenderer().tick();

		// Update particles subsystem if it exists
		if (getParticlesManager() != null && getParticlesManager() instanceof ClientParticlesRenderer)
			((ClientParticlesRenderer) getParticlesManager()).updatePhysics();
	}
}
