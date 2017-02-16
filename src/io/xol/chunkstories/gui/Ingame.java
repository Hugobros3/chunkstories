package io.xol.chunkstories.gui;

import io.xol.engine.math.Math2;

import java.util.Iterator;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityLiving.HitBox;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.KeyboardKeyInput;
import io.xol.chunkstories.api.item.Inventory;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.ClientMasterPluginManager;
import io.xol.chunkstories.client.ClientSlavePluginManager;
import io.xol.chunkstories.client.LocalServerContext;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.events.CameraSetupEvent;
import io.xol.chunkstories.core.events.PlayerLogoutEvent;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.gui.Chat.ChatPanelOverlay;
import io.xol.chunkstories.gui.overlays.ingame.DeathOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.gui.overlays.ingame.PauseOverlay;
import io.xol.chunkstories.input.lwjgl2.Lwjgl2ClientInputsManager;
import io.xol.chunkstories.item.inventory.InventoryAllVoxels;
import io.xol.chunkstories.item.renderer.InventoryDrawer;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.renderer.chunks.ChunkRenderable;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Ingame extends OverlayableScene
{
	final private WorldClientCommon world;
	
	//Moved from client to IG, as these make sense per world/play
	private final ClientPluginManager pluginManager;
	//private final Lwjgl2ClientInputsManager inputsManager;
	
	//Only in SP
	private final LocalServerContext localServer;

	// Renderer
	SelectionRenderer selectionRenderer;
	InventoryDrawer inventoryDrawer;

	Camera camera = new Camera();
	public Chat chat;
	protected boolean focus = true;
	Entity playerEntity;

	private boolean guiHidden = false;
	boolean shouldTakeACubemap = false;

	public Ingame(GameWindowOpenGL window, WorldClientCommon world)
	{
		super(window);
		this.world = world;
		window.renderingContext.setCamera(camera);
		
		chat = new Chat(this);
		
		if(world instanceof WorldMaster)
		{
			localServer = new LocalServerContext(Client.getInstance());
			pluginManager = new ClientMasterPluginManager(localServer);
		}
		else
		{
			localServer = null;
			pluginManager = new ClientSlavePluginManager(Client.getInstance());
		}
		
		//Hacky job because the client is a global state and the ingame scene is per-world
		Client.getInstance().setClientPluginManager(pluginManager);
		pluginManager.reloadPlugins();
		
		//Spawn manually the player if we're in Singleplayer
		if (world instanceof WorldMaster)
			world.spawnPlayer(Client.getInstance().getPlayer());

		//Creates the rendering stuff
		world.getWorldRenderer().setupRenderSize(window.getWidth(), window.getHeight());
		selectionRenderer = new SelectionRenderer(world);

		//Give focus
		focus(true);
	}

	public World getWorld()
	{
		return world;
	}

	public boolean hasFocus()
	{
		if (this.currentOverlay != null)
			return false;
		return focus;
	}

	float pauseOverlayFade = 0.0f;

	@Override
	public void update(RenderingContext renderingContext)
	{
		// Update client entity
		if ((playerEntity == null || playerEntity != Client.getInstance().getPlayer().getControlledEntity()) && Client.getInstance().getPlayer().getControlledEntity() != null)
		{
			playerEntity = Client.getInstance().getPlayer().getControlledEntity();
			if (playerEntity instanceof EntityWithSelectedItem)
				inventoryDrawer = ((EntityWithSelectedItem) playerEntity).getInventory() == null ? null : new InventoryDrawer((EntityWithSelectedItem) playerEntity);
			else
				inventoryDrawer = null;
		}

		if (playerEntity != null && ((EntityLiving) playerEntity).isDead() && !(this.currentOverlay instanceof DeathOverlay))
			this.changeOverlay(new DeathOverlay(this, null));

		//Get the player location
		Vector3dm cameraPosition = renderingContext.getCamera().getCameraPosition();

		// Update the player
		if (playerEntity instanceof EntityControllable)
			((EntityControllable) playerEntity).setupCamera(Client.getInstance().getPlayer());
		

		Location selectedBlock = null;
		if (playerEntity instanceof EntityPlayer)
			selectedBlock = ((EntityPlayer) playerEntity).getBlockLookingAt(true);

		if (playerEntity != null)
			playerEntity.setupCamera(camera);
		
		pluginManager.fireEvent(new CameraSetupEvent(renderingContext.getCamera()));

		//Main render call
		world.getWorldRenderer().renderWorldAtCamera(camera);

		//Debug draws
		if (RenderingConfig.physicsVisualization && playerEntity != null)
		{
			int id, data;
			int drawDebugDist = 6;
			//cameraPosition.negate();

			for (int i = ((int)(double) cameraPosition.getX()) - drawDebugDist; i <= ((int)(double) cameraPosition.getX()) + drawDebugDist; i++)
				for (int j = ((int)(double) cameraPosition.getY()) - drawDebugDist; j <= ((int)(double) cameraPosition.getY()) + drawDebugDist; j++)
					for (int k = ((int)(double) cameraPosition.getZ()) - drawDebugDist; k <= ((int)(double) cameraPosition.getZ()) + drawDebugDist; k++)
					{
						data = world.getVoxelData(i, j, k);
						id = VoxelFormat.id(data);
						
						CollisionBox[] tboxes = VoxelsStore.get().getVoxelById(id).getTranslatedCollisionBoxes(world, i, j, k);
						if (tboxes != null)
							for (CollisionBox box : tboxes)
								if (VoxelsStore.get().getVoxelById(id).getType().isSolid())
									box.debugDraw(1, 0, 0, 1.0f);
								else
									box.debugDraw(1, 1, 0, 0.25f);
						//VoxelsStore.get().getVoxelById(id).debugRenderCollision(world, i, j, k);
					}

			//player.getTranslatedBoundingBox().debugDraw(0, 1, 1, 1);

			Iterator<Entity> ie = world.getAllLoadedEntities();
			while (ie.hasNext())
			{
				Entity e = ie.next();
				
				if(e instanceof EntityLiving)
				{
					EntityLiving eli = (EntityLiving)e;
					for(HitBox hitbox: eli.getHitBoxes())
					{
						hitbox.draw(renderingContext);
					}
				}
				
				if(e.getTranslatedBoundingBox().lineIntersection(cameraPosition, camera.getViewDirection().castToDoublePrecision()) != null)
					e.getTranslatedBoundingBox().debugDraw(0, 0, 0.5f, 1);
				else
					e.getTranslatedBoundingBox().debugDraw(0, 1, 1, 1);
				
				//[Vector3dm x:67.29906576230833 y:23.65 z:28.805621056886654]
				//System.out.println(cameraPosition);
				
				for(CollisionBox box : e.getCollisionBoxes())
				{
					box.translate(e.getLocation());
					box.debugDraw(0, 1, 0.5f, 1);
				}
			}
		}
		
		if (selectedBlock != null && playerEntity instanceof EntityCreative && ((EntityCreative) playerEntity).getCreativeModeComponent().get())
			selectionRenderer.drawSelectionBox(selectedBlock);
		
		//Cubemap rendering trigger (can't run it while main render is occuring)
		if (shouldTakeACubemap)
		{
			shouldTakeACubemap = false;
			world.getWorldRenderer().renderWorldCubemap(null, 512, false);
		}
		//Blit the final 3d image
		world.getWorldRenderer().blitScreen(pauseOverlayFade);

		//Fades in & out the overlay
		if (this.currentOverlay == null)
		{
			if (pauseOverlayFade > 0.0)
				pauseOverlayFade -= 0.1;
		}
		else if (this.currentOverlay != null)
		{
			float maxFade = 1.0f;
			if (this.currentOverlay instanceof ChatPanelOverlay)
				maxFade = 0.25f;
			if (pauseOverlayFade < maxFade)
				pauseOverlayFade += 0.1;
		}

		//Draw the GUI
		if (!guiHidden)
		{
			//Draw chat
			chat.update();
			chat.draw();

			//Draw inventory
			if (playerEntity != null && inventoryDrawer != null)
				inventoryDrawer.drawPlayerInventorySummary(gameWindow.renderingContext, renderingContext.getWindow().getWidth() / 2 - 7, 64 + 64);

			//TODO : move this crap into the EntityOverlays shit
			//Draw health
			if (playerEntity != null && playerEntity instanceof EntityLiving)
			{
				EntityLiving livingPlayer = (EntityLiving) playerEntity;

				float scale = 2.0f;

				TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png").setLinearFiltering(false);
				renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderingContext.getWindow().getWidth() / 2 - 256 * 0.5f * scale, 64 + 64 + 16 - 32 * 0.5f * scale, 256 * scale, 32 * scale, 0, 32f / 256f, 1, 0,
						TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, null);

				//Health
				int horizontalBitsToDraw = (int) (8 + 118 * Math2.clamp(livingPlayer.getHealth() / livingPlayer.getMaxHealth(), 0.0, 1.0));
				renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderingContext.getWindow().getWidth() / 2 - 128 * scale, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0, 64f / 256f, horizontalBitsToDraw / 256f,
						32f / 256f, TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4fm(1.0f, 1.0f, 1.0f, 0.75f));

				//Food
				if (livingPlayer instanceof EntityPlayer)
				{
					EntityPlayer playerPlayer = (EntityPlayer)livingPlayer;
					
					horizontalBitsToDraw = (int) (0 + 126 * Math2.clamp(playerPlayer.getFoodLevel() / 100f, 0.0, 1.0));
					renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(
							renderingContext.getWindow().getWidth() / 2 + 0 * 128 * scale + 0, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0.5f , 64f / 256f, 0.5f + horizontalBitsToDraw / 256f,
							32f / 256f, TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4fm(1.0f, 1.0f, 1.0f, 0.75f));
				}
			}

			// Draw current overlay
			if (currentOverlay != null)
				currentOverlay.drawToScreen(renderingContext, 0, 0, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight());
			//Or draw cursor
			else
				renderingContext.getGuiRenderer().renderTexturedRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, 16, 16, 0, 0, 16, 16, 16, "gui/cursor");

			//Draw debug info
			if (RenderingConfig.showDebugInfo)
				drawF3debugMenu(renderingContext);
		}
		//Lack of overlay should infer autofocus
		if (currentOverlay == null && !chat.chatting)
			focus(true);

		Client.profiler.reset("gui");

		// Check connection didn't died and change scene if it has
		if (world instanceof WorldClientRemote)
		{

			if (!((WorldClientRemote) world).getConnection().isAlive() || ((WorldClientRemote) world).getConnection().hasFailed())
				Client.getInstance().exitToMainMenu("Connection terminated : " + ((WorldClientRemote) world).getConnection().getLatestErrorMessage());

		}

		//Auto-switch to pause if it detects the game isn't in focus anymore
		if (!Display.isActive() && this.currentOverlay == null)
		{
			focus(false);
			this.changeOverlay(new PauseOverlay(this, currentOverlay));
		}
	}

	public void focus(boolean f)
	{
		Mouse.setGrabbed(f);
		if (f && !focus)
		{
			Mouse.setCursorPosition(gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			this.changeOverlay(null);
		}
		focus = f;
	}

	public boolean onKeyRepeatEvent(int keyCode)
	{
		if (currentOverlay != null && currentOverlay instanceof ChatPanelOverlay)
		{
			ChatPanelOverlay chatPanel = (ChatPanelOverlay) currentOverlay;
			return chatPanel.handleKeypress(keyCode);
		}

		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode)
	{
		if (currentOverlay != null && currentOverlay.handleKeypress(keyCode))
			return true;

		KeyboardKeyInput keyBind = Client.getInstance().getInputsManager().getKeyBoundForLWJGL2xKey(keyCode);

		if (!guiHidden && keyBind != null)
		{
			//Block inputs if chatting
			if (Client.getInstance().getInputsManager().getInputByName("chat").equals(keyBind))
			{
				this.changeOverlay(chat.new ChatPanelOverlay(this, null));
				focus(false);
				return true;
			}

			if(Client.getInstance().getInputsManager().onInputPressed(keyBind) == true)
				return true;
		}

		//Function keys
		if (keyCode == Keyboard.KEY_F1)
		{
			guiHidden = !guiHidden;
		}
		else if (keyCode == Keyboard.KEY_F2)
		{
			chat.insert(world.getWorldRenderer().screenShot());
		}
		else if (keyCode == Keyboard.KEY_F3)
		{
			RenderingConfig.showDebugInfo = !RenderingConfig.showDebugInfo;
		}
		else if (keyCode == Keyboard.KEY_F4)
		{

		}
		else if (keyCode == Keyboard.KEY_F6)
		{

		}
		else if (keyCode == Keyboard.KEY_F8)
			shouldTakeACubemap = true;
		//CTRL-F12 reloads
		else if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == Keyboard.KEY_F12)
		{
			//Rebuild the mod FS
			Client.getInstance().reloadAssets();
			
			//Reload plugins
			this.pluginManager.reloadPlugins();
			
			//Mark some caches dirty
			world.getWorldRenderer().reloadContentSpecificStuff();
		}
		//CTRL-R redraws chunks
		else if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == 19)
		{
			((ParticlesRenderer) world.getParticlesManager()).cleanAllParticles();
			world.redrawEverything();
			world.getWorldRenderer().chunksRenderer.clear();
			ChunksRenderer.renderStart = System.currentTimeMillis();
			world.getWorldRenderer().flagModified();
		}
		//Item slots selection
		else if (keyBind != null && keyBind.getName().startsWith("inventorySlot"))
		{
			int requestedInventorySlot = Integer.parseInt(keyBind.getName().replace("inventorySlot", ""));
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
		else if (!guiHidden && Client.getInstance().getInputsManager().getInputByName("inventory").equals(keyBind))
		{
			if (playerEntity != null)
			{
				focus(false);
				if (playerEntity instanceof EntityCreative && ((EntityCreative) playerEntity).getCreativeModeComponent().get())
					this.changeOverlay(new InventoryOverlay(this, null, new Inventory[] { ((EntityWithInventory) playerEntity).getInventory(), new InventoryAllVoxels() }));
				else
					this.changeOverlay(new InventoryOverlay(this, null, new Inventory[] { ((EntityWithInventory) playerEntity).getInventory() }));
			}
		}
		//Exit brings up the pause menu
		else if (Client.getInstance().getInputsManager().getInputByName("exit").equals(keyBind))
		{
			focus(false);
			guiHidden = false;
			this.changeOverlay(new PauseOverlay(this, null));
		}
		return false;
	}

	public boolean onKeyUp(int keyCode)
	{
		KeyboardKeyInput keyBind = Client.getInstance().getInputsManager().getKeyBoundForLWJGL2xKey(keyCode);

		if (keyBind != null)
		{
			if(Client.getInstance().getInputsManager().onInputReleased(keyBind) == true)
				return true;
		}

		return false;
	}

	@Override
	public boolean onMouseButtonDown(int x, int y, int button)
	{
		if (currentOverlay != null)
			return currentOverlay.onClick(x, y, button);

		if (playerEntity == null)
			return false;

		Input mButton = null;
		switch (button)
		{
		case 0:
			mButton = Lwjgl2ClientInputsManager.LEFT;
			break;
		case 1:
			mButton = Lwjgl2ClientInputsManager.RIGHT;
			break;
		case 2:
			mButton = Lwjgl2ClientInputsManager.MIDDLE;
			break;
		}
		
		if (mButton != null)
			Client.getInstance().getInputsManager().onInputPressed(mButton);
		
		//TODO it does not handle the special clicks yet, maybye do it somewhere else, like in binds ?
		return false;
	}

	public boolean onMouseButtonUp(int x, int y, int button)
	{
		Input mButton = null;
		switch (button)
		{
		case 0:
			mButton = Lwjgl2ClientInputsManager.LEFT;
			break;
		case 1:
			mButton = Lwjgl2ClientInputsManager.RIGHT;
			break;
		case 2:
			mButton = Lwjgl2ClientInputsManager.MIDDLE;
			break;
		}
		
		if (mButton != null)
			Client.getInstance().getInputsManager().onInputReleased(mButton);

		return false;
	}

	@Override
	public boolean onScroll(int a)
	{
		if (currentOverlay != null && currentOverlay.onScroll(a))
			return true;
		//Scroll trought the items
		if (playerEntity != null && playerEntity instanceof EntityWithSelectedItem)
		{
			ItemPile selected = null;
			int selectedInventorySlot = ((EntityWithSelectedItem) playerEntity).getSelectedItemComponent().getSelectedSlot();
			int originalSlot = selectedInventorySlot;
			if (a < 0)
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
		return true;
	}

	@Override
	public void onResize()
	{
		world.getWorldRenderer().setupRenderSize(gameWindow.getWidth(), gameWindow.getHeight());
	}

	/**
	 * Destroys and frees everything
	 */
	@Override
	public void destroy()
	{
		//Logout sequence
		if(world instanceof WorldMaster)
		{
			Player player = Client.getInstance().getPlayer();
			
			PlayerLogoutEvent playerDisconnectionEvent = new PlayerLogoutEvent(player);
			pluginManager.fireEvent(playerDisconnectionEvent);
	
			if(this.playerEntity != null)
			{
				SerializedEntityFile playerEntityFile = new SerializedEntityFile("./players/" + Client.getInstance().getPlayer().getName().toLowerCase() + ".csf");
				playerEntityFile.write(this.playerEntity);
			}
		}
		//player.save();
		//player.removePlayerFromWorld();
		
		//Disables plugins
		pluginManager.disablePlugins();
		
		this.world.getWorldRenderer().destroy();
	}

	private void drawF3debugMenu(RenderingInterface renderingInterface)
	{
		int timeTook = Client.profiler.timeTook();
		String debugInfo = Client.profiler.reset("gui").toString();
		if (timeTook > 400)
			System.out.println("Lengty frame, printing debug information : \n" + debugInfo);

		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		int bx = ((int)(double) camera.getCameraPosition().getX());
		int by = ((int)(double) camera.getCameraPosition().getY());
		int bz = ((int)(double) camera.getCameraPosition().getZ());
		int data = world.getVoxelData(bx, by, bz);
		int bl = (data & 0x0F000000) >> 0x18;
		int sl = (data & 0x00F00000) >> 0x14;
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx, bz);

		float angleX = -1;
		if (playerEntity != null && playerEntity instanceof EntityLiving)
			angleX = Math.round(((EntityLiving) playerEntity).getEntityRotationComponent().getHorizontalRotation());
		//float angleY = Math.round(((EntityLiving) player).getEntityRotationComponent().getVerticalRotation());
		double dx = Math.sin(angleX / 360 * 2.0 * Math.PI);
		double dz = Math.cos(angleX / 360 * 2.0 * Math.PI);

		VoxelSides side = VoxelSides.TOP;

		//System.out.println("dx: "+dx+" dz:" + dz);

		if (Math.abs(dx) > Math.abs(dz))
		{
			if (dx > 0)
				side = VoxelSides.RIGHT;
			else
				side = VoxelSides.LEFT;
		}
		else
		{
			if (dz > 0)
				side = VoxelSides.FRONT;
			else
				side = VoxelSides.BACK;
		}

		//Location selectedBlockLocation = ((EntityControllable) player).getBlockLookingAt(false);

		int ec = 0;
		IterableIterator<Entity> i = world.getAllLoadedEntities();
		while (i.hasNext())
		{
			i.next();
			ec++;
		}

		Chunk current = world.getChunk(cx, cy, cz);
		int x_top = renderingInterface.getWindow().getHeight() - 16;
		FontRenderer2.drawTextUsingSpecificFont(20,
				x_top - 1 * 16, 0, 16, GLCalls.getStatistics() + " Chunks in view : " + formatBigAssNumber("" + world.getWorldRenderer().renderedChunks) + " Entities " + ec + " Particles :" + ((ParticlesRenderer) world.getParticlesManager()).count()
						+ " #FF0000Render FPS: " + Client.getInstance().getWindows().getFPS() + " avg: " + Math.floor(10000.0 / Client.getInstance().getWindows().getFPS()) / 10.0 + " #00FFFFSimulation FPS: " + world.getWorldRenderer().getWorld().getGameLogic().getSimulationFps(),
				BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 2 * 16, 0, 16, "Frame timings : " + debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 3 * 16, 0, 16, "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used, chunks loaded in ram: " + world.getRegionsHolder().countChunksWithData() + "/"
				+ world.getRegionsHolder().countChunks() + " " + Math.floor(world.getRegionsHolder().countChunksWithData() * 4 * 32 * 32 * 32 / (1024L * 1024 / 100f)) / 100f + "Mb used by chunks"

		, BitmapFont.SMALLFONTS);

		//FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + getLoadedChunksVramFootprint() + ", " + getLoadedTerrainVramFootprint(), BitmapFont.SMALLFONTS);

		long totalVram = (renderingInterface.getTotalVramUsage()) / 1024 / 1024;
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + totalVram + "Mb as " + Texture2D.getTotalNumberOfTextureObjects() + " textures using " + Texture2D.getTotalVramUsage() / 1024 / 1024 + "Mb + "
				+ VerticesObject.getTotalNumberOfVerticesObjects() + " Vertices objects using " + renderingInterface.getVertexDataVramUsage() / 1024 / 1024 + " Mb", BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 5 * 16, 0, 16, "Chunks to bake : " + world.getWorldRenderer().chunksRenderer.todoQueue.size() + " - " + world.ioHandler.toString(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 6 * 16, 0, 16,
				"Position : x:" + bx + " y:" + by + " z:" + bz + " dir: " + angleX + " side: " + side + " Block looking at : bl:" + bl + " sl:" + sl + " cx:" + cx + " cy:" + cy + " cz:" + cz + " csh:" + csh, BitmapFont.SMALLFONTS);

		if (current == null)
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk null", BitmapFont.SMALLFONTS);
		else if (current instanceof ChunkRenderable)
		{
			ChunkRenderData chunkRenderData = ((ChunkRenderable) current).getChunkRenderData();
			if (chunkRenderData != null)
			{
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk : " + current + " - " + chunkRenderData.toString(), BitmapFont.SMALLFONTS);
			}
			else
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk : " + current + " - No rendering data", BitmapFont.SMALLFONTS);
		}

		if (playerEntity != null && playerEntity instanceof EntityLiving)
		{
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 8 * 16, 0, 16, "Current Region : " + this.playerEntity.getWorld().getRegionChunkCoordinates(cx, cy, cz), BitmapFont.SMALLFONTS);
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 9 * 16, 0, 16, "Controlled Entity : " + this.playerEntity, BitmapFont.SMALLFONTS);
		}
	}

	@SuppressWarnings("unused")
	private String getLoadedChunksVramFootprint()
	{
		int nbChunks = 0;
		long octelsTotal = 0;

		ChunksIterator i = world.getAllLoadedChunks();
		Chunk c;
		while (i.hasNext())
		{
			c = i.next();
			if (c == null)
				continue;
			if (c instanceof ChunkRenderable)
			{
				ChunkRenderData chunkRenderData = ((ChunkRenderable) c).getChunkRenderData();
				if (chunkRenderData != null)
				{
					nbChunks++;
					//octelsTotal += chunkRenderData.getVramUsage();
				}
			}
		}
		return nbChunks + " chunks";//, storing " + octelsTotal / 1024 / 1024 + "Mb of vertex data.";
	}

	@SuppressWarnings("unused")
	private String getLoadedTerrainVramFootprint()
	{
		int nbChunks = world.getRegionsSummariesHolder().countSummaries();
		long octelsTotal = nbChunks * 256 * 256 * (1 + 1) * 4;

		return nbChunks + " regions, storing " + octelsTotal / 1024 / 1024 + "Mb of data";
	}

	public String formatBigAssNumber(String in)
	{
		String formatted = "";
		for (int i = 0; i < in.length(); i++)
		{
			if (i > 0 && i % 3 == 0)
				formatted = "." + formatted;
			formatted = in.charAt(in.length() - i - 1) + formatted;
		}
		return formatted;
	}
	
	public ClientPluginManager getPluginManager()
	{
		return pluginManager;
	}
}
