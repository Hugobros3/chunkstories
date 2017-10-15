package io.xol.chunkstories.gui;

import io.xol.engine.base.GameWindowOpenGL_LWJGL3;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.events.player.PlayerLogoutEvent;
import io.xol.chunkstories.api.events.rendering.CameraSetupEvent;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.ClientSlavePluginManager;
import io.xol.chunkstories.client.LocalServerContext;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.gui.Chat.ChatPanelOverlay;
import io.xol.chunkstories.gui.overlays.ingame.DeathOverlay;
import io.xol.chunkstories.gui.overlays.ingame.PauseOverlay;
import io.xol.chunkstories.renderer.decals.VoxelOverlays;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Ingame extends Layer
{
	private final WorldClientCommon world;
	
	//Moved from client to IG, as these make sense per world/play
	private final ClientPluginManager pluginManager;
	
	//Only in SP
	private final LocalServerContext localServer;

	// Renderer & client interface components
	private final VoxelOverlays selectionRenderer;
	private InventoryDrawer inventoryBarDrawer = null;
	private final PhysicsWireframeDebugger wireframeDebugger;
	private final DebugInfoRenderer debugInfoRenderer;
	public final Chat chatManager;
	
	//Convinience references
	private boolean focus2 = true;
	private Entity playerEntity;

	//TODO: Move to config, just like f3
	private boolean guiHidden = false;
	
	//Hack
	private boolean shouldTakeACubemap = false;

	public Ingame(GameWindowOpenGL_LWJGL3 window, WorldClientCommon world)
	{
		super(window, null);
		this.world = world;
		
		this.chatManager = new Chat(this);

		//Creates the rendering stuff
		this.selectionRenderer = new VoxelOverlays(world);
		this.wireframeDebugger = new PhysicsWireframeDebugger(window.getClient(), world);
		this.debugInfoRenderer = new DebugInfoRenderer(window.getClient(), world);
		
		//Start a mini server
		if(world instanceof WorldMaster)
		{
			localServer = new LocalServerContext(Client.getInstance());
			pluginManager = localServer.getPluginManager();
		}
		else
		{
			localServer = null;
			pluginManager = new ClientSlavePluginManager(Client.getInstance());
		}
		
		//Hacky job because the client is a global state and the ingame scene is per-world
		//Client.getInstance().setClientPluginManager(pluginManager);
		//pluginManager.reloadPlugins();
		
		//Spawn manually the player if we're in Singleplayer
		if (world instanceof WorldMaster)
			world.spawnPlayer(Client.getInstance().getPlayer());

		//Give focus
		focus(true);
	}

	public World getWorld()
	{
		return world;
	}
	
	public boolean isCovered() {
		return !gameWindow.getLayer().equals(this);
	}
	
	public boolean hasFocus()
	{
		if(isCovered())
		//if(gameWindow.getLayer() != this)
		//if (this.currentOverlay != null)
			return false;
		return focus2;
	}

	float pauseOverlayFade = 0.0f;

	@Override
	public void render(RenderingInterface renderingContext)
	{
		// Update client entity
		if ((playerEntity == null || playerEntity != Client.getInstance().getPlayer().getControlledEntity()) && Client.getInstance().getPlayer().getControlledEntity() != null)
		{
			playerEntity = Client.getInstance().getPlayer().getControlledEntity();
			
			
			if (playerEntity instanceof EntityWithSelectedItem)
				inventoryBarDrawer = ((EntityWithSelectedItem) playerEntity).getInventory() == null ? null : new InventoryDrawer((EntityWithSelectedItem) playerEntity);
			else
				inventoryBarDrawer = null;
		}
		
		if (playerEntity != null && ((EntityLiving) playerEntity).isDead() && !(gameWindow.getLayer() instanceof DeathOverlay))
			gameWindow.setLayer(new DeathOverlay(gameWindow, this));

		// Update the player
		if (playerEntity instanceof EntityControllable)
			((EntityControllable) playerEntity).onEachFrame(Client.getInstance().getPlayer());
		

		Location selectedBlock = null;
		if (playerEntity instanceof EntityPlayer)
			selectedBlock = ((EntityPlayer) playerEntity).getBlockLookingAt(true);
		
		pluginManager.fireEvent(new CameraSetupEvent(renderingContext.getCamera()));

		//Main render call
		world.getWorldRenderer().renderWorld(renderingContext);

		//Debug draws
		if (RenderingConfig.physicsVisualization && playerEntity != null) {
			wireframeDebugger.render(renderingContext);
		}
		
		if (!guiHidden && selectedBlock != null && playerEntity instanceof EntityCreative && ((EntityCreative) playerEntity).getCreativeModeComponent().get())
			selectionRenderer.drawSelectionBox(selectedBlock);
		
		selectionRenderer.drawnCrackedBlocks(renderingContext);
		
		//Cubemap rendering trigger (can't run it while main render is occuring)
		//TODO reimplement cubemaps screenshots
		if (shouldTakeACubemap)
		{
			shouldTakeACubemap = false;
			world.getWorldRenderer().getCubemapRenderer().renderWorldCubemap(renderingContext, null, 1024, false);
		}

		//Fades in & out the overlay
		if (!isCovered())
		{
			if (pauseOverlayFade > 0.0)
				pauseOverlayFade -= 0.1;
		}
		else
		{
			float maxFade = 1.0f;
			if (gameWindow.getLayer() instanceof ChatPanelOverlay)
				maxFade = 0.25f;
			if (pauseOverlayFade < maxFade)
				pauseOverlayFade += 0.1;
		}
		
		//Blit the final 3d image
		world.getWorldRenderer().blitFinalImage(renderingContext);

		//Draw the GUI
		if (!guiHidden)
		{
			//Draw chat
			chatManager.update();
			chatManager.draw(renderingContext);

			//Draw inventory
			if (playerEntity != null && inventoryBarDrawer != null)
				inventoryBarDrawer.drawPlayerInventorySummary(renderingContext, renderingContext.getWindow().getWidth() / 2 - 7, 64 + 64);

			//Draw debug info
			if (RenderingConfig.showDebugInfo)
				debugInfoRenderer.drawF3debugMenu(renderingContext);
			
			renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(getGameWindow().getWidth() / 2 - 8, getGameWindow().getHeight() / 2 - 8, 16, 16, 0, 1, 1, 0,
					renderingContext.textures().getTexture("./textures/gui/cursor.png"), false, true, null);
		}
		
		//Lack of overlay should infer autofocus
		if (!isCovered())
			focus(true);

		// Check connection didn't died and change scene if it has
		if (world instanceof WorldClientRemote)
		{
			if (!((WorldClientRemote) world).getConnection().isAlive() || ((WorldClientRemote) world).getConnection().hasFailed())
				Client.getInstance().exitToMainMenu("Connection terminated : " + ((WorldClientRemote) world).getConnection().getLatestErrorMessage());
		}

		//Auto-switch to pause if it detects the game isn't in focus anymore
		if (!gameWindow.hasFocus() && !isCovered())
		{
			focus(false);
			gameWindow.setLayer(new PauseOverlay(gameWindow, gameWindow.getLayer()));
		}
	}

	public void focus(boolean f)
	{
		gameWindow.getInputsManager().getMouse().setGrabbed(f);
		if (f && !focus2)
			gameWindow.getInputsManager().getMouse().setMouseCursorLocation(Math.floor(gameWindow.getWidth() / 2.0f), Math.floor(gameWindow.getHeight() / 2.0f));
		focus2 = f;
	}

	@Override
	public boolean handleInput(Input input)
	{
		if (!guiHidden)// && keyBind != null)
		{
			//Block inputs if chatting
			if (input.equals("chat"))
			{
				gameWindow.setLayer(chatManager.new ChatPanelOverlay(gameWindow, this));
				focus(false);
				return true;
			}

			//if(Client.getInstance().getInputsManager().onInputPressed(keyBind) == true)
			//	return true;
		}

		//Function keys
		if (input.equals("hideGui"))
		{
			guiHidden = !guiHidden;
		}
		else if (input.equals("screenshot"))
		{
			chatManager.insert(world.getWorldRenderer().screenShot());
		}
		else if (input.equals("toggleDebugInfo"))
		{
			gameWindow.getClient().configDeprecated().setString("showDebugInfo", gameWindow.getClient().configDeprecated().getBoolean("showDebugInfo", false) ? "false" : "true");
			RenderingConfig.define();
			//RenderingConfig.showDebugInfo = !RenderingConfig.showDebugInfo;
		}
		else if (input.equals("takeCubemap"))
			shouldTakeACubemap = true;
		//CTRL-F12 reloads
		else if (input.equals("reloadContent"))//(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == Keyboard.KEY_F12)
		{
			//Rebuild the mod FS
			Client.getInstance().reloadAssets();
			
			//Reload plugins
			this.pluginManager.reloadPlugins();
			
			//Mark some caches dirty
			world.getWorldRenderer().reloadContentSpecificStuff();
		}
		//CTRL-R redraws chunks
		else if (input.equals("redrawChunks"))//(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == 19)
		{
			System.out.println("Wow");
			((ClientParticlesRenderer) world.getParticlesManager()).cleanAllParticles();
			world.redrawEverything();
			world.getWorldRenderer().flagChunksModified();
			return true;
		}
		//Item slots selection
		else if (input.getName().startsWith("inventorySlot"))
		{
			int requestedInventorySlot = Integer.parseInt(input.getName().replace("inventorySlot", ""));
			//Match zero onto last slot
			if (requestedInventorySlot == 0)
				requestedInventorySlot = 10;

			//Map to zero-indexed inventory
			requestedInventorySlot--;

			if (playerEntity != null && playerEntity instanceof EntityWithSelectedItem)
			{
				//Do not accept request to select non-existent inventories slots
				if (requestedInventorySlot > ((EntityWithInventory) playerEntity).getInventory().getWidth())
					return false;

				ItemPile p = ((EntityWithInventory) playerEntity).getInventory().getItemPileAt(requestedInventorySlot, 0);
				if (p != null)
					requestedInventorySlot = p.getX();
				((EntityWithSelectedItem) playerEntity).getSelectedItemComponent().setSelectedSlot(requestedInventorySlot);

				return true;
			}
		}
		//Exit brings up the pause menu
		else if (input.equals("exit"))
		{
			focus(false);
			guiHidden = false;
			gameWindow.setLayer(new PauseOverlay(gameWindow, this));
		}
		else if(input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll)input;
			
			if (playerEntity != null && playerEntity instanceof EntityWithSelectedItem)
			{
				ItemPile selected = null;
				int selectedInventorySlot = ((EntityWithSelectedItem) playerEntity).getSelectedItemComponent().getSelectedSlot();
				int originalSlot = selectedInventorySlot;
				if (ms.amount() < 0)
				{
					selectedInventorySlot %= ((EntityWithInventory) playerEntity).getInventory().getWidth();
					selected = ((EntityWithInventory) playerEntity).getInventory().getItemPileAt(selectedInventorySlot, 0);
					if (selected != null)
						selectedInventorySlot += selected.getItem().getType().getSlotsWidth();
					else
						selectedInventorySlot++;
				}
				else
				{
					selectedInventorySlot--;
					if (selectedInventorySlot < 0)
						selectedInventorySlot += ((EntityWithInventory) playerEntity).getInventory().getWidth();
					selected = ((EntityWithInventory) playerEntity).getInventory().getItemPileAt(selectedInventorySlot, 0);
					if (selected != null)
						selectedInventorySlot = selected.getX();
				}
				//Switch slot
				if (originalSlot != selectedInventorySlot)
					((EntityWithSelectedItem) playerEntity).getSelectedItemComponent().setSelectedSlot(selectedInventorySlot);
			}
		}
		return false;
	}

	@Override
	public void onResize(int newWidth, int newHeight) {
		world.getWorldRenderer().setupRenderSize();
	}

	/**
	 * Destroys and frees everything
	 */
	@Override
	public void destroy()
	{
		//Logout sequence: Save the player entity
		if(world instanceof WorldMaster)
		{
			Player player = Client.getInstance().getPlayer();
			
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(player);
			pluginManager.fireEvent(playerDisconnectionEvent);
	
			if(this.playerEntity != null)
			{
				SerializedEntityFile playerEntityFile = new SerializedEntityFile(world.getFolderPath() + "/players/" + Client.getInstance().getPlayer().getName().toLowerCase() + ".csf");
				playerEntityFile.write(this.playerEntity);
			}
		}
		
		//Stop the game logic and save
		if(world instanceof WorldMaster) {
			
			//TODO: Stop simulation
			Fence fence = ((WorldMaster)world).stopLogic();
			
			//exitButton.text = "#{world.saving}";
			
			fence.traverse();
			fence = world.saveEverything();
			
			//exitButton.text = "#{world.saving}";
			
			fence.traverse();
		}
		
		//Disables plugins
		pluginManager.disablePlugins();
		
		this.world.getWorldRenderer().destroy();
	}
	
	public ClientPluginManager getPluginManager()
	{
		return pluginManager;
	}

	public float getPauseOverlayFade() {
		return pauseOverlayFade;
	}
}
