package io.xol.chunkstories.gui;

import io.xol.engine.math.Math2;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.Vector4f;

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
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.core.events.CameraSetupEvent;
import io.xol.chunkstories.gui.Chat.ChatPanelOverlay;
import io.xol.chunkstories.gui.overlays.ingame.DeathOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.gui.overlays.ingame.PauseOverlay;
import io.xol.chunkstories.input.lwjgl2.Lwjgl2ClientInputsManager;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.inventory.InventoryAllVoxels;
import io.xol.chunkstories.item.renderer.InventoryDrawer;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.renderer.chunks.ChunkRenderable;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Ingame extends OverlayableScene
{
	final private WorldClientCommon world;

	// Renderer
	public WorldRenderer worldRenderer;
	SelectionRenderer selectionRenderer;
	InventoryDrawer inventoryDrawer;

	Camera camera = new Camera();
	public Chat chat;
	protected boolean focus = true;
	Entity player;

	private boolean guiHidden = false;
	boolean shouldTakeACubemap = false;

	public Ingame(GameWindowOpenGL window, WorldClientCommon world)
	{
		super(window);
		this.world = world;
		window.renderingContext.setCamera(camera);

		//Spawn manually the player if we're in Singleplayer
		//TODO this should be managed by a proper localhost server rather than this appalling hack
		if (world instanceof WorldMaster)
		{
			//TODO remember a proper spawn location
			Client.getInstance().getClientSideController().setControlledEntity(new EntityPlayer(world, 0, 100, 0, Client.username));

			((EntityControllable) Client.getInstance().getClientSideController().getControlledEntity()).getControllerComponent().setController(Client.getInstance().getClientSideController());
			world.addEntity(Client.getInstance().getClientSideController().getControlledEntity());
		}

		//Creates the rendering stuff
		worldRenderer = new WorldRenderer(world);
		worldRenderer.setupRenderSize(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		selectionRenderer = new SelectionRenderer(world);

		chat = new Chat(this);

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
		if ((player == null || player != Client.getInstance().getClientSideController().getControlledEntity()) && Client.getInstance().getClientSideController().getControlledEntity() != null)
		{
			player = Client.getInstance().getClientSideController().getControlledEntity();
			if (player instanceof EntityWithSelectedItem)
				inventoryDrawer = ((EntityWithSelectedItem) player).getInventory() == null ? null : new InventoryDrawer((EntityWithSelectedItem) player);
			else
				inventoryDrawer = null;
		}

		if (player != null && ((EntityLiving) player).isDead() && !(this.currentOverlay instanceof DeathOverlay))
			this.changeOverlay(new DeathOverlay(this, null));

		//Get the player location
		Vector3dm cameraPosition = renderingContext.getCamera().getCameraPosition();

		// Update the player
		if (player instanceof EntityControllable)
			((EntityControllable) player).setupCamera(Client.getInstance().getClientSideController());
		

		Location selectedBlock = null;
		if (player instanceof EntityPlayer)
			selectedBlock = ((EntityPlayer) player).getBlockLookingAt(true);

		if (player != null)
			player.setupCamera(camera);
		
		Client.getInstance().getPluginManager().fireEvent(new CameraSetupEvent(renderingContext.getCamera()));

		//Main render call
		worldRenderer.renderWorldAtCamera(camera);

		if (selectedBlock != null && player instanceof EntityCreative && ((EntityCreative) player).getCreativeModeComponent().isCreativeMode())
			selectionRenderer.drawSelectionBox(selectedBlock);

		//Debug draws
		if (RenderingConfig.physicsVisualization && player != null)
		{
			int id, data;
			int drawDebugDist = 6;
			cameraPosition.negate();

			for (int i = ((int)(double) cameraPosition.getX()) - drawDebugDist; i <= ((int)(double) cameraPosition.getX()) + drawDebugDist; i++)
				for (int j = ((int)(double) cameraPosition.getY()) - drawDebugDist; j <= ((int)(double) cameraPosition.getY()) + drawDebugDist; j++)
					for (int k = ((int)(double) cameraPosition.getZ()) - drawDebugDist; k <= ((int)(double) cameraPosition.getZ()) + drawDebugDist; k++)
					{
						data = world.getVoxelData(i, j, k);
						id = VoxelFormat.id(data);
						Voxels.get(id).debugRenderCollision(world, i, j, k);
					}

			for (CollisionBox b : player.getTranslatedCollisionBoxes())
				b.debugDraw(0, 1, 1, 1);

			Iterator<Entity> ie = world.getAllLoadedEntities();
			while (ie.hasNext())
			{
				for (CollisionBox b : ie.next().getTranslatedCollisionBoxes())
					b.debugDraw(0, 1, 1, 1);
			}
		}
		//Cubemap rendering trigger (can't run it while main render is occuring)
		if (shouldTakeACubemap)
		{
			shouldTakeACubemap = false;
			worldRenderer.renderWorldCubemap(null, 512, false);
		}
		//Blit the final 3d image
		worldRenderer.blitScreen(pauseOverlayFade);

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
			if (player != null && inventoryDrawer != null)
				inventoryDrawer.drawPlayerInventorySummary(gameWindow.renderingContext, GameWindowOpenGL.windowWidth / 2 - 7, 64 + 64);

			//TODO : move this crap into the EntityOverlays shit
			//Draw health
			if (player != null && player instanceof EntityLiving)
			{
				EntityLiving livingPlayer = (EntityLiving) player;

				float scale = 2.0f;

				TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png").setLinearFiltering(false);
				renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(GameWindowOpenGL.windowWidth / 2 - 256 * 0.5f * scale, 64 + 64 + 16 - 32 * 0.5f * scale, 256 * scale, 32 * scale, 0, 32f / 256f, 1, 0,
						TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, null);

				//Health
				int horizontalBitsToDraw = (int) (8 + 118 * Math2.clamp(livingPlayer.getHealth() / livingPlayer.getMaxHealth(), 0.0, 1.0));
				renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(GameWindowOpenGL.windowWidth / 2 - 128 * scale, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0, 64f / 256f, horizontalBitsToDraw / 256f,
						32f / 256f, TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));

				//Food
				if (livingPlayer instanceof EntityPlayer)
				{
					EntityPlayer playerPlayer = (EntityPlayer)livingPlayer;
					
					horizontalBitsToDraw = (int) (0 + 126 * Math2.clamp(playerPlayer.getFoodLevel() / 100f, 0.0, 1.0));
					renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(
							GameWindowOpenGL.windowWidth / 2 + 0 * 128 * scale + 0, 64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0.5f , 64f / 256f, 0.5f + horizontalBitsToDraw / 256f,
							32f / 256f, TexturesHandler.getTexture("./textures/gui/hud/hud_survival.png"), false, true, new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));
				}
			}

			// Draw current overlay
			if (currentOverlay != null)
				currentOverlay.drawToScreen(renderingContext, 0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
			//Or draw cursor
			else
				renderingContext.getGuiRenderer().renderTexturedRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, 16, 16, 0, 0, 16, 16, 16, "gui/cursor");

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
			Mouse.setCursorPosition(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
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

		KeyBind keyBind = Client.getInstance().getInputsManager().getKeyBoundForLWJGL2xKey(keyCode);

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
			chat.insert(worldRenderer.screenShot());
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
			Client.getInstance().reloadAssets();
			worldRenderer.reloadContentSpecificStuff();
		}
		//CTRL-R redraws chunks
		else if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == 19)
		{
			((ParticlesRenderer) world.getParticlesManager()).cleanAllParticles();
			world.redrawEverything();
			worldRenderer.chunksRenderer.clear();
			ChunksRenderer.renderStart = System.currentTimeMillis();
			worldRenderer.flagModified();
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

			if (player != null && player instanceof EntityWithSelectedItem)
			{
				//Do not accept request to select non-existent inventories slots
				if (requestedInventorySlot > ((EntityWithInventory) player).getInventory().getWidth())
					return false;

				ItemPile p = ((EntityWithInventory) player).getInventory().getItemPileAt(requestedInventorySlot, 0);
				if (p != null)
					requestedInventorySlot = p.getX();
				((EntityWithSelectedItem) player).getSelectedItemComponent().setSelectedSlot(requestedInventorySlot);

				return true;
			}
		}
		else if (!guiHidden && Client.getInstance().getInputsManager().getInputByName("inventory").equals(keyBind))
		{
			if (player != null)
			{
				focus(false);
				if (player instanceof EntityCreative && ((EntityCreative) player).getCreativeModeComponent().isCreativeMode())
					this.changeOverlay(new InventoryOverlay(this, null, new Inventory[] { ((EntityWithInventory) player).getInventory(), new InventoryAllVoxels() }));
				else
					this.changeOverlay(new InventoryOverlay(this, null, new Inventory[] { ((EntityWithInventory) player).getInventory() }));
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
		KeyBind keyBind = Client.getInstance().getInputsManager().getKeyBoundForLWJGL2xKey(keyCode);

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

		if (player == null)
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
		if (player != null && player instanceof EntityWithSelectedItem)
		{
			ItemPile selected = null;
			int selectedInventorySlot = ((EntityWithSelectedItem) player).getSelectedItemComponent().getSelectedSlot();
			int originalSlot = selectedInventorySlot;
			if (a < 0)
			{
				selectedInventorySlot %= ((EntityWithInventory) player).getInventory().getWidth();
				selected = ((EntityWithInventory) player).getInventory().getItemPileAt(selectedInventorySlot, 0);
				if (selected != null)
					selectedInventorySlot += selected.getItem().getSlotsWidth();
				else
					selectedInventorySlot++;
			}
			else
			{
				selectedInventorySlot--;
				if (selectedInventorySlot < 0)
					selectedInventorySlot += ((EntityWithInventory) player).getInventory().getWidth();
				selected = ((EntityWithInventory) player).getInventory().getItemPileAt(selectedInventorySlot, 0);
				if (selected != null)
					selectedInventorySlot = selected.getX();
			}
			//Switch slot
			if (originalSlot != selectedInventorySlot)
				((EntityWithSelectedItem) player).getSelectedItemComponent().setSelectedSlot(selectedInventorySlot);
		}
		return true;
	}

	@Override
	public void onResize()
	{
		worldRenderer.setupRenderSize(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	}

	/**
	 * Destroys and frees everything
	 */
	@Override
	public void destroy()
	{
		this.worldRenderer.destroy();
	}

	private void drawF3debugMenu(RenderingInterface renderingInterface)
	{
		int timeTook = Client.profiler.timeTook();
		String debugInfo = Client.profiler.reset("gui").toString();
		if (timeTook > 400)
			System.out.println("Lengty frame, printing debug information : \n" + debugInfo);

		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		int bx = ((int)(double) camera.pos.getX());
		int by = ((int)(double) camera.pos.getY());
		int bz = ((int)(double) camera.pos.getZ());
		int data = world.getVoxelData(bx, by, bz);
		int bl = (data & 0x0F000000) >> 0x18;
		int sl = (data & 0x00F00000) >> 0x14;
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(bx, bz);

		float angleX = -1;
		if (player != null && player instanceof EntityLiving)
			angleX = Math.round(((EntityLiving) player).getEntityRotationComponent().getHorizontalRotation());
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
		int x_top = GameWindowOpenGL.windowHeight - 16;
		FontRenderer2.drawTextUsingSpecificFont(20,
				x_top - 1 * 16, 0, 16, GLCalls.getStatistics() + " Chunks in view : " + formatBigAssNumber("" + worldRenderer.renderedChunks) + " Entities " + ec + " Particles :" + ((ParticlesRenderer) world.getParticlesManager()).count()
						+ " #FF0000Render FPS: " + GameWindowOpenGL.getFPS() + " avg: " + Math.floor(10000.0 / GameWindowOpenGL.getFPS()) / 10.0 + " #00FFFFSimulation FPS: " + worldRenderer.getWorld().getGameLogic().getSimulationFps(),
				BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 2 * 16, 0, 16, "Frame timings : " + debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 3 * 16, 0, 16, "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used, chunks loaded in ram: " + world.getRegionsHolder().countChunksWithData() + "/"
				+ world.getRegionsHolder().countChunks() + " " + Math.floor(world.getRegionsHolder().countChunksWithData() * 4 * 32 * 32 * 32 / (1024L * 1024 / 100f)) / 100f + "Mb used by chunks"

		, BitmapFont.SMALLFONTS);

		//FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + getLoadedChunksVramFootprint() + ", " + getLoadedTerrainVramFootprint(), BitmapFont.SMALLFONTS);

		long totalVram = (renderingInterface.getTotalVramUsage()) / 1024 / 1024;
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + totalVram + "Mb as " + Texture2D.getTotalNumberOfTextureObjects() + " textures using " + Texture2D.getTotalVramUsage() / 1024 / 1024 + "Mb + "
				+ VerticesObject.getTotalNumberOfVerticesObjects() + " Vertices objects using " + renderingInterface.getVertexDataVramUsage() / 1024 / 1024 + " Mb", BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 5 * 16, 0, 16, "Chunks to bake : " + worldRenderer.chunksRenderer.todoQueue.size() + " - " + world.ioHandler.toString(), BitmapFont.SMALLFONTS);
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

		if (player != null && player instanceof EntityLiving)
		{
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 8 * 16, 0, 16, "Current Region : " + this.player.getWorld().getRegionChunkCoordinates(cx, cy, cz), BitmapFont.SMALLFONTS);
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 9 * 16, 0, 16, "Controlled Entity : " + this.player, BitmapFont.SMALLFONTS);
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
					octelsTotal += chunkRenderData.getVramUsage();
				}
			}
		}
		return nbChunks + " chunks, storing " + octelsTotal / 1024 / 1024 + "Mb of vertex data.";
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
}
