package io.xol.chunkstories.gui;

import java.util.Iterator;

/*import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;*/

import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.base.GameWindowOpenGL_LWJGL3;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityLiving.HitBox;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.events.player.PlayerLogoutEvent;
import io.xol.chunkstories.api.events.rendering.CameraSetupEvent;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.rendering.CameraInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.IterableIterator;
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
import io.xol.chunkstories.core.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.gui.Chat.ChatPanelOverlay;
import io.xol.chunkstories.gui.overlays.ingame.DeathOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.gui.overlays.ingame.PauseOverlay;
import io.xol.chunkstories.item.inventory.InventoryLocalCreativeMenu;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.chunks.RenderableChunk;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Ingame extends Layer
{
	final private WorldClientCommon world;
	
	//Moved from client to IG, as these make sense per world/play
	private final ClientPluginManager pluginManager;
	
	//Only in SP
	private final LocalServerContext localServer;

	// Renderer
	SelectionRenderer selectionRenderer;
	InventoryDrawer inventoryDrawer;

	//Camera camera = new Camera();
	public Chat chat;
	protected boolean focus = true;
	Entity playerEntity;

	private boolean guiHidden = false;
	boolean shouldTakeACubemap = false;

	public Ingame(GameWindowOpenGL_LWJGL3 window, WorldClientCommon world)
	{
		super(window, null);
		this.world = world;
		
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
		//world.getWorldRenderer().setupRenderSize(window.getWidth(), window.getHeight());
		selectionRenderer = new SelectionRenderer(world);

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
		return focus;
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
				inventoryDrawer = ((EntityWithSelectedItem) playerEntity).getInventory() == null ? null : new InventoryDrawer((EntityWithSelectedItem) playerEntity);
			else
				inventoryDrawer = null;
		}

		CameraInterface camera = renderingContext.getCamera();
		
		if (playerEntity != null && ((EntityLiving) playerEntity).isDead() && !(gameWindow.getLayer() instanceof DeathOverlay))
			gameWindow.setLayer(new DeathOverlay(gameWindow, this));

		//Get the player location
		Vector3dm cameraPosition = (Vector3dm) renderingContext.getCamera().getCameraPosition();

		// Update the player
		if (playerEntity instanceof EntityControllable)
			((EntityControllable) playerEntity).onEachFrame(Client.getInstance().getPlayer());
		

		Location selectedBlock = null;
		if (playerEntity instanceof EntityPlayer)
			selectedBlock = ((EntityPlayer) playerEntity).getBlockLookingAt(true);

		/*if (playerEntity != null)
			playerEntity.setupCamera(camera);*/
		
		pluginManager.fireEvent(new CameraSetupEvent(renderingContext.getCamera()));

		//Main render call
		world.getWorldRenderer().renderWorld(renderingContext);

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
									FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4fm(1, 0, 0, 1.0f));
									//box.debugDraw(1, 0, 0, 1.0f);
								else
									FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4fm(1, 1, 0, 0.25f));
									//box.debugDraw(1, 1, 0, 0.25f);
						
						//((VoxelTypeImplementation) VoxelsStore.get().getVoxelById(id).getType()).debugRenderCollision(renderingContext, world, i, j, k);
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

					FakeImmediateModeDebugRenderer.renderCollisionBox(e.getTranslatedBoundingBox(), new Vector4fm(0, 0, 0.5f, 1.0f));
					//e.getTranslatedBoundingBox().debugDraw(0, 0, 0.5f, 1);
				else
					FakeImmediateModeDebugRenderer.renderCollisionBox(e.getTranslatedBoundingBox(), new Vector4fm(0, 1f, 1f, 1.0f));
				//e.getTranslatedBoundingBox().debugDraw(0, 1, 1, 1);
				
				//[Vector3dm x:67.29906576230833 y:23.65 z:28.805621056886654]
				//System.out.println(cameraPosition);
				
				for(CollisionBox box : e.getCollisionBoxes())
				{
					box.translate(e.getLocation());
					FakeImmediateModeDebugRenderer.renderCollisionBox(box, new Vector4fm(0, 1, 0.5f, 1.0f));
					//box.debugDraw(0, 1, 0.5f, 1);
				}
			}
		}
		
		if (selectedBlock != null && playerEntity instanceof EntityCreative && ((EntityCreative) playerEntity).getCreativeModeComponent().get())
			selectionRenderer.drawSelectionBox(selectedBlock);
		
		//Cubemap rendering trigger (can't run it while main render is occuring)
		//TODO reimplement cubemaps screenshots
		/*if (shouldTakeACubemap)
		{
			shouldTakeACubemap = false;
			world.getWorldRenderer().renderWorldCubemap(null, 512, false);
		}*/

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
			chat.update();
			chat.draw(renderingContext);

			//Draw inventory
			if (playerEntity != null && inventoryDrawer != null)
				inventoryDrawer.drawPlayerInventorySummary(renderingContext, renderingContext.getWindow().getWidth() / 2 - 7, 64 + 64);

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
			//Or draw cursor
			//if(!isCovered())
				renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderingContext.getWindow().getWidth() / 2 - 8, renderingContext.getWindow().getHeight() / 2 - 8, 16, 16, 0, 1, 1, 0, renderingContext.textures().getTexture("./textures/gui/cursor.png"), false, true, null);

			//Draw debug info
			if (RenderingConfig.showDebugInfo)
				drawF3debugMenu(renderingContext);
		}
		//Lack of overlay should infer autofocus
		if (!isCovered())// && !chat.chatting)
		{
			focus(true);
		}
			
		Client.profiler.reset("gui");

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
		if (f && !focus)
			gameWindow.getInputsManager().getMouse().setMouseCursorLocation(Math.floor(gameWindow.getWidth() / 2.0f), Math.floor(gameWindow.getHeight() / 2.0f));
		focus = f;
	}

	@Override
	public boolean handleInput(Input input)
	{
		if (!guiHidden)// && keyBind != null)
		{
			//Block inputs if chatting
			if (input.equals("chat"))
			{
				gameWindow.setLayer(chat.new ChatPanelOverlay(gameWindow, this));
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
			chat.insert("Saved screenshot as "+world.getWorldRenderer().screenShot());
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
		else if (!guiHidden && input.equals("inventory"))
		{
			if (playerEntity != null)
			{
				focus(false);
				if (playerEntity instanceof EntityCreative && ((EntityCreative) playerEntity).getCreativeModeComponent().get())
					gameWindow.setLayer(new InventoryOverlay(gameWindow, this, new Inventory[] { ((EntityWithInventory) playerEntity).getInventory(), new InventoryLocalCreativeMenu() }));
				else
					gameWindow.setLayer(new InventoryOverlay(gameWindow, this, new Inventory[] { ((EntityWithInventory) playerEntity).getInventory() }));
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
		CameraInterface camera = renderingInterface.getCamera();
		
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
				x_top - 1 * 16, 0, 16, GLCalls.getStatistics() + " Chunks in view : " + formatBigAssNumber("" + world.getWorldRenderer().getChunkMeshesRenderer().getChunksVisibleForPass(WorldRenderer.RenderingPass.NORMAL_OPAQUE)) + " Entities " + ec + " Particles :" + ((ClientParticlesRenderer) world.getParticlesManager()).count()
						+ " #FF0000Render FPS: " + Client.getInstance().getGameWindow().getFPS() + " avg: " + Math.floor(10000.0 / Client.getInstance().getGameWindow().getFPS()) / 10.0 + " #00FFFFSimulation FPS: " + world.getWorldRenderer().getWorld().getGameLogic().getSimulationFps(),
				BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 2 * 16, 0, 16, "Frame timings : " + debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 3 * 16, 0, 16, "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used, chunks loaded in ram: " + world.getRegionsHolder().countChunksWithData() + "/"
				+ world.getRegionsHolder().countChunks() + " " + Math.floor(world.getRegionsHolder().countChunksWithData() * 4 * 32 * 32 * 32 / (1024L * 1024 / 100f)) / 100f + "Mb used by chunks"

		, BitmapFont.SMALLFONTS);

		//FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + getLoadedChunksVramFootprint() + ", " + getLoadedTerrainVramFootprint(), BitmapFont.SMALLFONTS);

		long totalVram = (renderingInterface.getTotalVramUsage()) / 1024 / 1024;
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + totalVram + "Mb as " + Texture2DGL.getTotalNumberOfTextureObjects() + " textures using " + Texture2DGL.getTotalVramUsage() / 1024 / 1024 + "Mb + "
				+ VertexBufferGL.getTotalNumberOfVerticesObjects() + " Vertices objects using " + renderingInterface.getVertexDataVramUsage() / 1024 / 1024 + " Mb", BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 5 * 16, 0, 16, "Chunks to bake : " + world.getWorldRenderer().getChunkMeshesRenderer().getBaker() + " - " + world.ioHandler.toString(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 6 * 16, 0, 16,
				"Position : x:" + bx + " y:" + by + " z:" + bz + " dir: " + angleX + " side: " + side + " Block looking at : bl:" + bl + " sl:" + sl + " cx:" + cx + " cy:" + cy + " cz:" + cz + " csh:" + csh, BitmapFont.SMALLFONTS);

		if (current == null)
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current Chunk null", BitmapFont.SMALLFONTS);
		else if (current instanceof ChunkRenderable)
		{
			ChunkRenderDataHolder chunkRenderData = ((RenderableChunk) current).getChunkRenderData();
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
				ChunkRenderDataHolder chunkRenderData = ((RenderableChunk) c).getChunkRenderData();
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

	public float getPauseOverlayFade() {
		return pauseOverlayFade;
	}
}
