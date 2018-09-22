//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitVoxelSelection;
import io.xol.chunkstories.api.entity.traits.TraitWhenControlled;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.entity.traits.serializable.TraitSelectedItem;
import io.xol.chunkstories.api.events.player.PlayerLogoutEvent;
import io.xol.chunkstories.api.events.rendering.CameraSetupEvent;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.Configuration.OptionBoolean;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.gui.InventoryGridRenderer;
import io.xol.chunkstories.gui.layer.ingame.ChatManager.ChatPanelOverlay;
import io.xol.chunkstories.renderer.debug.DebugInfoRenderer;
import io.xol.chunkstories.renderer.debug.PhysicsWireframeDebugger;
import io.xol.chunkstories.renderer.decals.VoxelOverlays;
import io.xol.chunkstories.renderer.particles.ClientParticlesRenderer;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

/**
 * The main layer that hosts the gameplay: renders the world, inventory and most
 * gui elements
 */
public class Ingame extends Layer {
	private final IngameClient client;
	private final LocalPlayer player;
	//private final WorldClientCommon world;

	// Renderer & client interface components
	//private final VoxelOverlays selectionRenderer;
	private InventoryGridRenderer inventoryBarDrawer = null;
	//private final PhysicsWireframeDebugger wireframeDebugger;
	//private final DebugInfoRenderer debugInfoRenderer;
	public final ChatManager chatManager;

	// Convinience references
	private boolean focus2 = true;
	private Entity playerEntity;

	// TODO: Move to config, just like f3
	private boolean guiHidden = false;

	float pauseOverlayFade = 0.0f;

	public Ingame(Gui window, IngameClient client) {
		super(window, null);
		this.client = client;
		this.player = client.player;
		
		this.chatManager = new ChatManager(this);

		// Give focus
		focus(true);
	}

	private boolean isCovered() {
		return gui.getTopLayer() != this;
	}

	public boolean hasFocus() {
		if (isCovered())
			return false;
		return focus2;
	}

	@Override
	public void render(GuiDrawer renderer) {
		// Update client entity
		if ((playerEntity == null || playerEntity != player.getControlledEntity())
				&& player.getControlledEntity() != null) {
			playerEntity = player.getControlledEntity();

			TraitInventory inv = playerEntity.traits.get(TraitInventory.class);
			if (inv != null)
				inventoryBarDrawer = new InventoryGridRenderer(inv);
			else
				inventoryBarDrawer = null;
		}

		// TODO MOVE MOVE MOVE
		if ((playerEntity != null && playerEntity.traits.tryWithBoolean(TraitHealth.class, eh -> eh.isDead()))
				&& !(gui.getTopLayer() instanceof DeathScreen))
			gui.setTopLayer(new DeathScreen(gui, this));

		// Update the player
		Location selectedBlock = null;
		if (playerEntity != null) {
			playerEntity.traits.with(TraitWhenControlled.class, twc -> twc.onEachFrame(player));
			selectedBlock = playerEntity.traits.tryWith(TraitVoxelSelection.class,
					tvs -> tvs.getBlockLookingAt(true, false));
		}

		world.getPluginManager().fireEvent(new CameraSetupEvent(renderer.getCamera()));

		// Main render call
		world.getWorldRenderer().renderWorld(renderer);

		// Debug draws
		if (client.getConfiguration().getBooleanOption("client.debug.physicsVisualization") && playerEntity != null) {
			wireframeDebugger.render(renderer);
		}

		if (!guiHidden && selectedBlock != null)
			selectionRenderer.drawSelectionBox(renderer, selectedBlock);

		// Fades in & out the overlay
		if (!isCovered()) {
			if (pauseOverlayFade > 0.0)
				pauseOverlayFade -= 0.1;
		} else {
			float maxFade = 1.0f;
			if (gameWindow.getLayer() instanceof ChatPanelOverlay)
				maxFade = 0.25f;
			if (pauseOverlayFade < maxFade)
				pauseOverlayFade += 0.1;
		}

		// Blit the final 3d image
		world.getWorldRenderer().blitFinalImage(renderer, guiHidden);

		// Draw the GUI
		if (!guiHidden) {
			chatManager.render(renderer);

			// Draw inventory
			if (inventoryBarDrawer != null)
				inventoryBarDrawer.drawPlayerInventorySummary(renderer, renderer.getWindow().getWidth() / 2 - 7,
						64 + 64);

			// Draw debug worldInfo
			if (client.getConfiguration().getBooleanOption("client.debug.showDebugInfo"))
				debugInfoRenderer.drawF3debugMenu(renderer);

			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(getGameWindow().getWidth() / 2 - 8,
					getGameWindow().getHeight() / 2 - 8, 16, 16, 0, 1, 1, 0,
					renderer.textures().getTexture("./textures/gui/cursor.png"), false, true, null);
		}

		// Lack of overlay should infer autofocus
		if (!isCovered())
			focus(true);

		// Check connection didn't died and change scene if it has
		if (world instanceof WorldClientRemote) {
			if (!((WorldClientRemote) world).getConnection().isOpen())
				gameWindow.getClient().exitToMainMenu("Connection terminated : " + "(TODO: not this way)");
		}

		// Auto-switch to pause if it detects the game isn't in focus anymore
		if (!gameWindow.hasFocus() && !isCovered()) {
			focus(false);
			gameWindow.setLayer(new PauseMenu(gameWindow, gameWindow.getLayer()));
		}
	}

	public void focus(boolean f) {
		gameWindow.getInputsManager().getMouse().setGrabbed(f);
		if (f && !focus2)
			gameWindow.getInputsManager().getMouse().setMouseCursorLocation(Math.floor(gameWindow.getWidth() / 2.0f),
					Math.floor(gameWindow.getHeight() / 2.0f));
		focus2 = f;
	}

	@Override
	public boolean handleInput(Input input) {
		// Block inputs if chatting
		if (input.equals("chat")) {
			gameWindow.setLayer(chatManager.new ChatPanelOverlay(gameWindow, this));
			focus(false);
			guiHidden = false;
			return true;
		} else if (input.equals("hideGui")) {
			guiHidden = !guiHidden;
			return true;
		} else if (input.equals("screenshot")) {
			chatManager.insert(world.getWorldRenderer().screenShot());
		} else if (input.equals("toggleDebugInfo")) {
			OptionBoolean debugInfo = (OptionBoolean) client.getConfiguration().getOption("client.debug.showDebugInfo");
			debugInfo.toggle();
			guiHidden = false;
			return true;
		} else if (input.equals("takeCubemap")) {
			// shouldTakeACubemap = true;
			return true;
			// CTRL-F12 reloads
		} else if (input.equals("reloadContent")) {
			// Rebuild the mod FS
			gameWindow.getClient().reloadAssets();

			// Reload plugins
			world.getPluginManager().reloadPlugins();

			// Mark some caches dirty
			world.getWorldRenderer().reloadContentSpecificStuff();
			return true;
			// CTRL-R redraws chunks
		} else if (input.equals("redrawChunks")) {
			((ClientParticlesRenderer) world.getParticlesManager()).cleanAllParticles();
			world.redrawEverything();
			world.getWorldRenderer().flagChunksModified();
			return true;
			// Item slots selection
		} else if (input.getName().startsWith("inventorySlot")) {
			int requestedInventorySlot = Integer.parseInt(input.getName().replace("inventorySlot", ""));
			// Match zero onto last slot
			if (requestedInventorySlot == 0)
				requestedInventorySlot = 10;

			// Map to zero-indexed inventory
			requestedInventorySlot--;

			if (playerEntity != null) {
				TraitInventory playerInventory = playerEntity.traits.get(TraitInventory.class);
				if (playerInventory == null)
					return false;

				// java lambda nonsense :(
				final int passedrequestedInventorySlot = requestedInventorySlot;
				return playerEntity.traits.tryWithBoolean(TraitSelectedItem.class, esi -> {
					// Do not accept request to select non-existent inventories slots
					int slot = passedrequestedInventorySlot;

					if (slot > playerInventory.getWidth())
						return false;

					ItemPile p = playerInventory.getItemPileAt(slot, 0);
					if (p != null)
						slot = p.getX();
					esi.setSelectedSlot(slot);
					return true;
				});
			}

			return false;
		} else if (input.equals("exit")) /* Exit brings up the pause menu */ {
			focus(false);
			guiHidden = false;
			gameWindow.setLayer(new PauseMenu(gameWindow, this));
			return true;
		} else if (input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll) input;

			if (playerEntity != null) {
				TraitInventory playerInventory = playerEntity.traits.get(TraitInventory.class);
				if (playerInventory == null)
					return false;

				playerEntity.traits.with(TraitSelectedItem.class, esi -> {
					ItemPile selected = null;
					int selectedInventorySlot = esi.getSelectedSlot();
					int originalSlot = selectedInventorySlot;
					if (ms.amount() < 0) {
						selectedInventorySlot %= playerInventory.getWidth();
						selected = playerInventory.getItemPileAt(selectedInventorySlot, 0);
						if (selected != null)
							selectedInventorySlot += selected.getItem().getDefinition().getSlotsWidth();
						else
							selectedInventorySlot++;
					} else {
						selectedInventorySlot--;
						if (selectedInventorySlot < 0)
							selectedInventorySlot += playerInventory.getWidth();
						selected = playerInventory.getItemPileAt(selectedInventorySlot, 0);
						if (selected != null)
							selectedInventorySlot = selected.getX();
					}
					// Switch slot
					if (originalSlot != selectedInventorySlot)
						esi.setSelectedSlot(selectedInventorySlot);

				});

				return true;
			}
		}
		return false;
	}

	@Override
	public void onResize(int newWidth, int newHeight) {
		//world.getWorldRenderer().setupRenderSize(newWidth, newHeight);
	}

	@Override
	public void destroy() {

	}

	public float getPauseOverlayFade() {
		return pauseOverlayFade;
	}
}
