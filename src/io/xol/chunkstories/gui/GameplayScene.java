package io.xol.chunkstories.gui;

import io.xol.engine.math.lalgb.Vector3f;

import java.util.Iterator;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import static org.lwjgl.opengl.GL11.*;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.FontRenderer2;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.core.ClientInputPressedEvent;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.input.MouseClick;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.gui.menus.InventoryOverlay;
import io.xol.chunkstories.gui.menus.PauseOverlay;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.item.inventory.InventoryAllVoxels;
import io.xol.chunkstories.item.renderer.InventoryDrawer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.physics.particules.ParticleLight;
import io.xol.chunkstories.physics.particules.ParticleSetupLight;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer;
import io.xol.chunkstories.renderer.lights.DefferedSpotLight;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GameplayScene extends OverlayableScene
{
	// Renderer
	public WorldRenderer worldRenderer;
	SelectionRenderer selectionRenderer;
	InventoryDrawer inventoryDrawer;

	Camera camera = new Camera();
	public ChatPanel chat = new ChatPanel();
	boolean focus = true;
	Entity player;

	public GameplayScene(XolioWindow w, boolean multiPlayer)
	{
		super(w);
		w.renderingContext.setCamera(camera);

		//We need a world to work on
		if (Client.world == null)
			w.changeScene(new MainMenu(w, false));

		//Spawn manually the player if we're in SP debug
		if (!multiPlayer)
		{
			Client.controlledEntity = new EntityPlayer(Client.world, 0, 100, 0, Client.username);
			((EntityControllable) Client.controlledEntity).setController(Client.getInstance());
			Client.world.addEntity(Client.controlledEntity);
		}

		//Creates the rendering stuff
		worldRenderer = new WorldRenderer(Client.world);
		worldRenderer.setupRenderSize(XolioWindow.frameW, XolioWindow.frameH);
		selectionRenderer = new SelectionRenderer(Client.world, worldRenderer);
		//Give focus
		focus(true);
	}

	public boolean hasFocus()
	{
		if (this.currentOverlay != null)
			return false;
		return focus;
	}

	//int selectedInventorySlot = 0;

	@Override
	public void update()
	{
		// Update client entity
		if (player == null || player != Client.controlledEntity && Client.controlledEntity != null)
		{
			player = Client.controlledEntity;
			inventoryDrawer = player.getInventory() == null ? null : new InventoryDrawer(player.getInventory());
		}
		inventoryDrawer.inventory = player.getInventory();
		

		//Get the player location
		Location loc = player.getLocation();

		// Update the player
		if (player instanceof EntityControllable)
			((EntityControllable) player).moveCamera(Client.clientController);

		Location selectedBlock = null;
		if (player instanceof EntityPlayer)
		{
			selectedBlock = ((EntityPlayer) player).getBlockLookingAt(true);
		}
		if (player != null)
			player.setupCamera(camera);
		//Temp
		if (flashLight)
		{
			float transformedViewH = (float) ((camera.view_rotx) / 180 * Math.PI);
			// System.out.println(Math.sin(transformedViewV)+"f");
			Vector3f viewerCamDirVector = new Vector3f((float) (Math.sin((-camera.view_roty) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)),
					(float) (Math.cos((-camera.view_roty) / 180 * Math.PI) * Math.cos(transformedViewH)));
			Vector3f lightPosition = new Vector3f((float) loc.x, (float) loc.y + (float) ((EntityPlayer) this.player).eyePosition, (float) loc.z);
			viewerCamDirVector.scale(-0.5f);
			Vector3f.add(viewerCamDirVector, lightPosition, lightPosition);
			viewerCamDirVector.scale(-1f);
			viewerCamDirVector.normalise();
			XolioWindow.getInstance().renderingContext.lights.add(new DefferedSpotLight(new Vector3f(1f, 1f, 0.9f), lightPosition, 35f, 35f, viewerCamDirVector));
			if (Keyboard.isKeyDown(Keyboard.KEY_F5))
				Client.world.particlesHolder
						.addParticle(new ParticleSetupLight(Client.world, loc.x, loc.y + 1.0f, loc.z, new DefferedSpotLight(new Vector3f(1f, 1f, 1f), new Vector3f((float) loc.x, (float) loc.y + 1.5f, (float) loc.z), 75f, 20f, viewerCamDirVector)));
		}
		//Main render call
		worldRenderer.renderWorldAtCamera(camera);

		if (selectedBlock != null)
			selectionRenderer.drawSelectionBox(selectedBlock);

		//Debug draws
		if (FastConfig.physicsVisualization && player != null)
		{
			int id, data;
			int drawDebugDist = 6;
			for (int i = ((int) loc.x) - drawDebugDist; i <= ((int) loc.x) + drawDebugDist; i++)
				for (int j = ((int) loc.y) - drawDebugDist; j <= ((int) loc.y) + drawDebugDist; j++)
					for (int k = ((int) loc.z) - drawDebugDist; k <= ((int) loc.z) + drawDebugDist; k++)
					{
						data = Client.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						VoxelTypes.get(id).debugRenderCollision(Client.world, i, j, k);
					}

			for (CollisionBox b : player.getTranslatedCollisionBoxes())
				b.debugDraw(0, 1, 1, 1);
			glDisable(GL_DEPTH_TEST);
			Iterator<Entity> ie = Client.world.getAllLoadedEntities();
			while (ie.hasNext())
				ie.next().debugDraw();
			glEnable(GL_DEPTH_TEST);
		}
		//Cubemap rendering trigger (can't run it while main render is occuring)
		if (shouldCM)
		{
			shouldCM = false;
			worldRenderer.screenCubeMap(512, null);
		}
		//Blit the final 3d image
		worldRenderer.postProcess();

		if (FastConfig.showDebugInfo)
			debug();
		else
			Client.profiler.reset("gui");

		chat.update();
		chat.draw();

		if (player != null && player.getInventory() != null)
			inventoryDrawer.drawPlayerInventorySummary(eng.renderingContext, XolioWindow.frameW / 2, 64 + 64);

		if (Keyboard.isKeyDown(78))
			Client.world.worldTime += 10;
		if (Keyboard.isKeyDown(74))
		{
			if (Client.world.worldTime > 10)
				Client.world.worldTime -= 10;
		}

		if (currentOverlay == null && !chat.chatting)
			focus(true);
		// Draw overlay
		if (currentOverlay != null)
			currentOverlay.drawToScreen(0, 0, XolioWindow.frameW, XolioWindow.frameH);
		else
			ObjectRenderer.renderTexturedRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, 16, 16, 0, 0, 16, 16, 16, "internal://./res/textures/gui/cursor.png");

		super.update();
		// Check connection didn't died and change scene if it has
		if (Client.connection != null)
		{
			if (!Client.connection.isAlive() || Client.connection.hasFailed())
				eng.changeScene(new MainMenu(eng, "Connection failed : " + Client.connection.getLatestErrorMessage()));
		}

		if (!Display.isActive() && this.currentOverlay == null)
		{
			focus(false);
			this.changeOverlay(new PauseOverlay(this, null));
		}
	}

	private void focus(boolean f)
	{
		Mouse.setGrabbed(f);
		if (f && !focus)
		{
			Mouse.setCursorPosition(XolioWindow.frameW / 2, XolioWindow.frameH / 2);
			this.changeOverlay(null);
		}
		focus = f;
	}

	boolean flashLight = false;
	boolean shouldCM = false;

	byte[] inventorySerialized;

	@Override
	public boolean onKeyPress(int k)
	{
		KeyBind keyBind = KeyBinds.getKeyBindForLWJGL2xKey(k);
		if (keyBind != null)
		{
			ClientInputPressedEvent event = new ClientInputPressedEvent(keyBind);

			Client.pluginsManager.fireEvent(event);
			if (event.isCancelled())
				return true;
			else if (((EntityControllable) this.player).handleInteraction(keyBind))
				return true;
		}

		Location loc = player.getLocation();
		if (currentOverlay != null && currentOverlay.handleKeypress(k))
			return true;
		if (!chat.chatting)
		{
			if (KeyBinds.getKeyBind("chat").equals(keyBind))
			{
				this.changeOverlay(chat.new ChatPanelOverlay(this, null));
				focus(false);
				return true;
			}
		}
		if (k == 19)
		{
			Client.world.particlesHolder.cleanAllParticles();
			Client.world.reRender();
			worldRenderer.chunksRenderer.clear();
			ChunksRenderer.renderStart = System.currentTimeMillis();
			worldRenderer.modified();
		}
		//TODO move this to core content plugin
		else if (KeyBinds.getKeyBind("use").equals(keyBind))
		{
			Client.getInstance().getSoundManager().playSoundEffect("sfx/flashlight.ogg", (float) loc.x, (float) loc.y, (float) loc.z, 1.0f, 1.0f);
			flashLight = !flashLight;
		}
		else if (KeyBinds.getKeyBind("inventory").equals(keyBind))
		{
			if (player != null)
			{
				focus(false);
				this.changeOverlay(new InventoryOverlay(this, null, new Inventory[] { player.getInventory(), new InventoryAllVoxels() }));
			}
		}
		else if (k == Keyboard.KEY_F1)
		{
			if (player instanceof EntityPlayer)
				((EntityPlayer) player).toogleFly();
		}
		else if (k == Keyboard.KEY_F2)
			chat.insert(worldRenderer.screenShot());
		else if (k == Keyboard.KEY_F3)
		{
			//Client.getSoundManager().playSoundEffect("music/menu3.ogg", (float)loc.x, (float)loc.y, (float)loc.z, 1.0f, 1.0f);
			//Client.getSoundManager().stopAnySound();
			//Client.getSoundManager().playMusic("music/radio/horse.ogg", (float) loc.x, (float) loc.y, (float) loc.z, 1.0f, 1.0f, false).setAttenuationEnd(50f);
		}
		else if (k == Keyboard.KEY_F4)
			Client.world.particlesHolder.addParticle(new ParticleLight(Client.world, loc.x + (Math.random() - 0.5) * 30, loc.y + (Math.random()) * 10, loc.z + (Math.random() - 0.5) * 30));

		else if (k == Keyboard.KEY_F6)
		{
			if (player instanceof EntityPlayer)
				((EntityPlayer) player).toggleNoclip();
		}
		else if (k == Keyboard.KEY_F8)
			shouldCM = true;
		else if (k == Keyboard.KEY_F12)
		{
			GameData.reload();
			GameData.reloadClientContent();
			worldRenderer.terrain.redoBlockTexturesSummary();
		}
		else if (KeyBinds.getKeyBind("exit").equals(keyBind))
		{
			focus(false);
			this.changeOverlay(new PauseOverlay(this, null));
		}
		return false;
	}

	@Override
	public boolean onClick(int x, int y, int button)
	{
		if (currentOverlay != null)
			return currentOverlay.onClick(x, y, button);

		if (player == null)
			return false;

		MouseClick mButton = null;
		switch (button)
		{
		case 0:
			mButton = MouseClick.LEFT;
			break;
		case 1:
			mButton = MouseClick.RIGHT;
			break;
		case 2:
			mButton = MouseClick.MIDDLE;
			break;
		}
		if (mButton != null)
		{
			ClientInputPressedEvent event = new ClientInputPressedEvent(mButton);
			if (mButton != null)
				Client.pluginsManager.fireEvent(event);
			if (!event.isCancelled())
				return ((EntityControllable) this.player).handleInteraction(mButton);
		}
		//TODO it does not handle the special clicks yet, maybye do it somewhere else, like in binds ?
		return false;
	}

	@Override
	public boolean onScroll(int a)
	{
		if (currentOverlay != null && currentOverlay.onScroll(a))
			return true;
		//Scroll trought the items
		if (player != null && player.getInventory() != null)
		{
			ItemPile selected = null;
			int selectedInventorySlot = player.getInventory().getSelectedSlot();
			int originalSlot = selectedInventorySlot;
			if (a < 0)
			{
				selectedInventorySlot %= player.getInventory().width;
				selected = player.getInventory().getItem(selectedInventorySlot, 0);
				if (selected != null)
					selectedInventorySlot += selected.item.getSlotsWidth();
				else
					selectedInventorySlot++;
			}
			else
			{
				selectedInventorySlot--;
				if (selectedInventorySlot < 0)
					selectedInventorySlot += player.getInventory().width;
				selected = player.getInventory().getItem(selectedInventorySlot, 0);
				if (selected != null)
					selectedInventorySlot = selected.x;
			}
			//Switch slot
			if (originalSlot != selectedInventorySlot)
				player.getInventory().setSelectedSlot(selectedInventorySlot);
		}
		return true;
	}

	@Override
	public void onResize()
	{
		worldRenderer.setupRenderSize(XolioWindow.frameW, XolioWindow.frameH);
	}

	/**
	 * Destroys and frees everything
	 */
	@Override
	public void destroy()
	{
		Client.world.destroy();
		this.worldRenderer.destroy();
		if (Client.connection != null)
		{
			Client.connection.close();
			Client.connection = null;
		}
	}

	private void debug()
	{
		int timeTook = Client.profiler.timeTook();
		String debugInfo = Client.profiler.reset("gui");
		if (timeTook > 400)
			System.out.println("Lengty frame, printing debug information : \n" + debugInfo);

		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		int bx = (-(int) camera.pos.x);
		int by = (-(int) camera.pos.y);
		int bz = (-(int) camera.pos.z);
		int data = Client.world.getDataAt(bx, by, bz);
		int bl = (data & 0x0F000000) >> 0x18;
		int sl = (data & 0x00F00000) >> 0x14;
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = Client.world.regionSummaries.getHeightAt(bx, bz);
		CubicChunk current = Client.world.getChunk(cx, cy, cz, false);
		int x_top = XolioWindow.frameH - 16;
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 1 * 16, 0, 16, "View distance : " + FastConfig.viewDistance + " Vertices(N):" + formatBigAssNumber(worldRenderer.renderedVertices + "") + " Chunks in view : "
				+ formatBigAssNumber("" + worldRenderer.renderedChunks) + " Particles :" + Client.world.particlesHolder.count() + " #FF0000FPS : " + XolioWindow.getFPS(), BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 2 * 16, 0, 16, "Timings : " + debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 3 * 16, 0, 16, "RAM usage : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used, chunks loaded in ram: " + Client.world.chunksData.size(), BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 4 * 16, 0, 16, "VRAM usage : " + getLoadedChunksVramFootprint() + ", " + getLoadedTerrainVramFootprint(), BitmapFont.SMALLFONTS);

		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 5 * 16, 0, 16,
				"Chunks to bake : T : " + worldRenderer.chunksRenderer.todoQueue.size() + "   Chunks to upload: " + worldRenderer.chunksRenderer.doneQueue.size() + "    " + Client.world.ioHandler.toString(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 6 * 16, 0, 16, "Position : x:" + bx + " y:" + by + " z:" + bz + " bl:" + bl + " sl:" + sl + " cx:" + cx + " cy:" + cy + " cz:" + cz + " csh:" + csh, BitmapFont.SMALLFONTS);
		if (current == null)
			FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current chunk null", BitmapFont.SMALLFONTS);
		else
		{
			ChunkRenderData chunkRenderData = current.chunkRenderData;
			if (chunkRenderData != null)
			{
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current chunk : " + current + " - " + chunkRenderData.toString(), BitmapFont.SMALLFONTS);
			}
			else
				FontRenderer2.drawTextUsingSpecificFont(20, x_top - 7 * 16, 0, 16, "Current chunk : " + current + " - No rendering data", BitmapFont.SMALLFONTS);
		}
		FontRenderer2.drawTextUsingSpecificFont(20, x_top - 8 * 16, 0, 16, "Controller : " + this.player, BitmapFont.SMALLFONTS);

	}

	private String getLoadedChunksVramFootprint()
	{
		int nbChunks = 0;
		long octelsTotal = 0;

		ChunksIterator i = Client.world.iterator();
		CubicChunk c;
		while (i.hasNext())
		{
			c = i.next();
			if (c == null)
				continue;
			ChunkRenderData chunkRenderData = c.chunkRenderData;
			if (chunkRenderData != null)
			{
				nbChunks++;
				octelsTotal += chunkRenderData.getVramUsage();
			}
		}
		return nbChunks + " chunks, storing " + octelsTotal / 1024 / 1024 + "Mb of vertex data.";
	}

	private String getLoadedTerrainVramFootprint()
	{
		int nbChunks = Client.world.regionSummaries.all().size();
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
