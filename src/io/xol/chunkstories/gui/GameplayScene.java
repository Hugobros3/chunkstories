package io.xol.chunkstories.gui;

import org.lwjgl.util.vector.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.chunkstories.GameData;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.gui.menus.InventoryDrawer;
import io.xol.chunkstories.gui.menus.InventoryOverlay;
import io.xol.chunkstories.gui.menus.PauseOverlay;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.physics.particules.ParticleLight;
import io.xol.chunkstories.physics.particules.ParticleSetupLight;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.ChunksRenderer;
import io.xol.chunkstories.renderer.DefferedLight;
import io.xol.chunkstories.renderer.EntityRenderer;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class GameplayScene extends OverlayableScene
{
	// Renderer
	public WorldRenderer worldRenderer;
	EntityRenderer entityRenderer;
	InventoryDrawer inventoryDrawer;

	Camera camera = new Camera();
	public ChatPanel chat = new ChatPanel();
	boolean focus = true;

	public boolean multiPlayer;
	EntityImplementation player;

	public GameplayScene(XolioWindow w, boolean multiPlayer)
	{
		super(w);

		this.multiPlayer = multiPlayer;

		if (Client.world == null)
			w.changeScene(new MainMenu(w, false));

		if (!multiPlayer)
		{
			Client.controller = new EntityPlayer(Client.world, 0, 100, 0, Client.username);
			Client.world.addEntity(Client.controller);
		}

		worldRenderer = new WorldRenderer(Client.world);
		worldRenderer.setupRenderSize(XolioWindow.frameW, XolioWindow.frameH);
		entityRenderer = new EntityRenderer(Client.world, worldRenderer);

		focus(true);
	}
	
	 int selectedInventorySlot = 0;

	public void update()
	{
		// Update client entity
		if (player == null || player != Client.controller && Client.controller != null)
		{
			player = (EntityImplementation) Client.controller;
			inventoryDrawer = player.inventory == null ? null : new InventoryDrawer(player.inventory);
		}
		inventoryDrawer.inventory = player.inventory;
		// Update the player
		if (player instanceof EntityControllable)
			((EntityControllable) player).controls(focus);

		/*int[] selectedBlock = null;
		if (player instanceof EntityPlayer)
		{
			selectedBlock = ((EntityPlayer) player).rayTraceSelectedBlock(true);
		}*/
		if (player != null)
			player.setupCamera(camera);
		else
			camera.justSetup();
		if (flashLight)
		{
			float transformedViewH = (float) ((camera.view_rotx) / 180 * Math.PI);
			// System.out.println(Math.sin(transformedViewV)+"f");
			Vector3f viewerCamDirVector = new Vector3f((float) (Math.sin((-camera.view_roty) / 180 * Math.PI) * Math.cos(transformedViewH)), (float) (Math.sin(transformedViewH)),
					(float) (Math.cos((-camera.view_roty) / 180 * Math.PI) * Math.cos(transformedViewH)));

			worldRenderer.lights.add(new DefferedLight(new Vector3f(1f, 1f, 0.9f), new Vector3f((float) player.posX, (float) player.posY + 1.5f, (float) player.posZ), 75f, 40f, viewerCamDirVector));
			if (Keyboard.isKeyDown(Keyboard.KEY_F5))
				Client.world.particlesHolder.addParticle(new ParticleSetupLight(Client.world, player.posX, player.posY + 1.0f, player.posZ, new DefferedLight(new Vector3f(1f, 1f, 1f), new Vector3f((float) player.posX, (float) player.posY + 1.5f,
						(float) player.posZ), 75f, 40f, viewerCamDirVector)));
		}
		worldRenderer.renderWorldAtCamera(camera);
		//if (selectedBlock != null)
		//	entityRenderer.drawSelectionBox(selectedBlock[0], selectedBlock[1], selectedBlock[2]);
		
		if (FastConfig.physicsVisualization && player != null)
		{
			int id, data;
			int drawDebugDist = 6;
			for (int i = ((int) player.posX) - drawDebugDist; i <= ((int) player.posX) + drawDebugDist; i++)
				for (int j = ((int) player.posY) - drawDebugDist; j <= ((int) player.posY) + drawDebugDist; j++)
					for (int k = ((int) player.posZ) - drawDebugDist; k <= ((int) player.posZ) + drawDebugDist; k++)
					{
						data = Client.world.getDataAt(i, j, k);
						id = VoxelFormat.id(data);
						VoxelTypes.get(id).debugRenderCollision(Client.world, i, j, k);
					}

			for (CollisionBox b : player.getTranslatedCollisionBoxes())
				b.debugDraw(0, 1, 1, 1);
		}
		
		if (shouldCM)
		{
			shouldCM = false;
			worldRenderer.screenCubeMap(512, null);
		}
		// THEN THE GUI
		worldRenderer.postProcess();
		
		if (FastConfig.showDebugInfo)
			debug();
		else
			Client.profiler.reset("gui");
		 
		chat.update();
		chat.draw();

		if (player != null && player.inventory != null)
				inventoryDrawer.drawPlayerInventorySummary(XolioWindow.frameW / 2, 64 + 64, selectedInventorySlot);

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
			
		super.update();
		// Check connection didn't died and change scene if it has
		if (Client.connection != null)
		{
			if (!Client.connection.isAlive() || Client.connection.hasFailed())
				eng.changeScene(new MainMenu(eng, "Connection failed : " + Client.connection.getLatestErrorMessage()));
		}
	}

	private void focus(boolean f)
	{
		focus = f;
		Mouse.setGrabbed(f);
		if (f)
		{
			Mouse.setCursorPosition(XolioWindow.frameW / 2, XolioWindow.frameH / 2);
			this.changeOverlay(null);
		}
	}

	boolean flashLight = false;
	boolean shouldCM = false;

	byte[] inventorySerialized;
	
	public boolean onKeyPress(int k)
	{
		if (currentOverlay != null && currentOverlay.handleKeypress(k))
			return true;
		if (!chat.chatting)
		{
			if (k == FastConfig.CHAT_KEY)
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
		else if (k == FastConfig.GRABUSE_KEY)
		{
			Client.getSoundManager().playSoundEffect("sfx/flashlight.ogg", (float)player.posX, (float)player.posY, (float)player.posZ, 1.0f, 1.0f);

			flashLight = !flashLight;
		}
		else if (k == FastConfig.INVENTORY_KEY)
		{
			if (player != null)
			{
				focus(false);
				this.changeOverlay(new InventoryOverlay(this, null, new Inventory[]{player.inventory}));
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
			//Client.getSoundManager().playSoundEffect("music/menu3.ogg", (float)player.posX, (float)player.posY, (float)player.posZ, 1.0f, 1.0f);
			Client.getSoundManager().stopAnySound();
			Client.getSoundManager().playMusic("music/radio/horse.ogg", (float)player.posX, (float)player.posY, (float)player.posZ, 1.0f, 1.0f, false).setAttenuationEnd(50f);
		}
		else if (k == Keyboard.KEY_F4)
			Client.world.particlesHolder.addParticle(new ParticleLight(Client.world, player.posX + (Math.random() - 0.5) * 30, player.posY + (Math.random()) * 10, player.posZ + (Math.random() - 0.5) * 30));
		
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
		else if (k == FastConfig.EXIT_KEY)
		{
			focus(false);
			this.changeOverlay(new PauseOverlay(this, null));
		}
		return false;
	}

	public boolean onClick(int x, int y, int button)
	{
		if (currentOverlay != null)
			return currentOverlay.onClick(x, y, button);
		if (!(player instanceof EntityPlayer))
			return false;

		//EntityPlayer player2 = (EntityPlayer) player;
		/*if (button == 1)
		{
			int[] selectedBlock = player2.rayTraceSelectedBlock(false);
			if (selectedBlock != null)
			{
				Client.world.setDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2], VoxelFormat.format(voxelId, meta, 0, 0), true);
				worldRenderer.modified();
			}
		}
		else if (button == 0)
		{
			int[] selectedBlock = player2.rayTraceSelectedBlock(true);
			if (selectedBlock != null)
			{
				Client.world.setDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2], 0, true);
				worldRenderer.modified();
			}
		}
		else if (button == 2)
		{
			int[] selectedBlock = player2.rayTraceSelectedBlock(true);
			if (selectedBlock != null)
			{
				int data = Client.world.getDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2]);
				voxelId = VoxelFormat.id(data);
				meta = VoxelFormat.meta(data);
			}
		}*/
		return false;
	}

	public boolean onScroll(int a)
	{
		if (currentOverlay != null && currentOverlay.onScroll(a))
			return true;
		//Scroll trought the items
		if(player != null && player.inventory != null)
		{
			ItemPile selected = null;
			if(a < 0)
			{
				selectedInventorySlot %= player.inventory.width;
				selected = player.inventory.getItem(selectedInventorySlot, 0);
				if(selected != null)
					selectedInventorySlot+= selected.item.getSlotsWidth();
				else
					selectedInventorySlot++;
			}
			else
			{
				selectedInventorySlot--;
				if(selectedInventorySlot < 0)
					selectedInventorySlot += player.inventory.width;
				selected = player.inventory.getItem(selectedInventorySlot, 0);
				if(selected != null)
					selectedInventorySlot = selected.x;
			}
		}
		return true;
	}

	public void onResize()
	{
		worldRenderer.setupRenderSize(XolioWindow.frameW, XolioWindow.frameH);
	}

	// CLEANING - do it properly or mum will smash the shit out of you

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
			System.out.println(debugInfo);

		long total = Runtime.getRuntime().totalMemory();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		int bx = (-(int) camera.camPosX);
		int by = (-(int) camera.camPosY);
		int bz = (-(int) camera.camPosZ);
		int data = Client.world.getDataAt(bx, by, bz);
		int bl = (data & 0x0F000000) >> 0x18;
		int sl = (data & 0x00F00000) >> 0x14;
		int cx = bx / 32;
		int cy = by / 32;
		int cz = bz / 32;
		int csh = Client.world.chunkSummaries.getHeightAt(bx, bz);
		CubicChunk current = Client.world.getChunk(cx, cy, cz, false);
		// FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 20,
		// 0, 16, "X:" + player.posX + " Y:" + player.posY + " Z:" + player.posZ
		// + "rotH" + camera.view_roty + " worldtime:" + Client.world.worldTime
		// % 1000, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 36, 0, 16, "Position : x:" + bx + " y:" + by + " z:" + bz + " bl:" + bl + " sl:" + sl + " cx:" + cx + " cy:" + cy + " cz:" + cz + " csh:" + csh, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 52, 0, 16, "CR : T : " + worldRenderer.chunksRenderer.todo.size() + " D: " + worldRenderer.chunksRenderer.done.size() + "WL : " + Client.world.ioHandler.toString()
				+ " ChunksData" + Client.world.chunksData.size() + " WR list:" + worldRenderer.getQueueSize(), BitmapFont.SMALLFONTS);
		if (current == null)
			FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 68, 0, 16, "Current chunk null", BitmapFont.SMALLFONTS);
		else
			FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 68, 0, 16, "Current chunk : vbo=" + current.vbo_id + " vboSize=" + (current.vbo_size_normal + current.vbo_size_water) + " needRender=" + current.need_render + " requestable=" + current.requestable
					+ " dataPointer=" + current.dataPointer + " etc "+current+" etc2"+current.holder, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 84, 0, 16, debugInfo, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 100, 0, 16, "View distance : " + FastConfig.viewDistance + " Vertices(N):" + formatBigAssNumber(worldRenderer.renderedVertices + "") + " Chunks in view : "
				+ formatBigAssNumber("" + worldRenderer.renderedChunks) + " Particles :" + Client.world.particlesHolder.count() + " #FF0000FPS : " + XolioWindow.getFPS(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 114, 0, 16, used / 1024 / 1024 + " / " + total / 1024 / 1024 + " mb used", BitmapFont.SMALLFONTS);
		
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 130, 0, 16, "VRAM usage : " + getLoadedChunksVramFootprint() + ", " + getLoadedTerrainVramFootprint(), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(20, XolioWindow.frameH - 130 - 16, 0, 16, "Player model : " + this.player, BitmapFont.SMALLFONTS);
		 
		if (!Display.isActive() && this.currentOverlay == null)
		{
			focus(false);
			this.changeOverlay(new PauseOverlay(this, null));
		}
	}

	private String getLoadedChunksVramFootprint()
	{
		int nbChunks = 0;
		long octelsTotal = 0;

		ChunksIterator i = Client.world.iterator();
		CubicChunk c;
		while(i.hasNext())
		{
			c = i.next();

			if(c == null)
				continue;
			nbChunks++;
			octelsTotal += c.vbo_size_normal * 16 + (c.vbo_size_water + c.vbo_size_complex) * 24;	
		}
		return nbChunks + " chunks, storing " + octelsTotal / 1024 / 1024 + "Mb of vertex data.";
	}

	private String getLoadedTerrainVramFootprint()
	{
		int nbChunks = Client.world.chunkSummaries.all().size();
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
