//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.TraitVoxelSelection;
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable;
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.entity.traits.serializable.TraitSelectedItem;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.util.Configuration.OptionBoolean;
import io.xol.chunkstories.client.ingame.IngameClientImplementation;
import io.xol.chunkstories.gui.InventoryGridRenderer;
import io.xol.chunkstories.gui.layer.ingame.ChatManager.ChatPanelOverlay;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientRemote;

/**
 * The main layer that hosts the gameplay: renders the world, inventory and most
 * gui elements
 */
public class IngameLayer extends Layer {
	private final IngameClientImplementation client;
	private final LocalPlayer player;
	private final WorldClientCommon world;

	// Renderer & client interface components
	//private final VoxelOverlays selectionRenderer;
	private InventoryGridRenderer inventoryBarDrawer = null;
	//private final PhysicsWireframeDebugger wireframeDebugger;
	//private final DebugInfoRenderer debugInfoRenderer;
	public final ChatManager chatManager;

	//private Entity playerEntity;

	// TODO: Move to config, just like f3
	private boolean guiHidden = false;

	float pauseOverlayFade = 0.0f;

	public IngameLayer(Gui window, IngameClientImplementation client) {
		super(window, null);
		this.client = client;
		this.player = client.getPlayer();
		this.world = client.getWorld();
		
		this.chatManager = new ChatManager(client, this);

		// Give focus
		focus(true);
	}

	private boolean isCovered() {
		return gui.getTopLayer() != this;
	}

	public boolean hasFocus() {
		if (isCovered())
			return false;
		return gui.getMouse().isGrabbed();
	}

	@Override
	public void render(GuiDrawer renderer) {
		// Update client entity
		Entity playerEntity = player.getControlledEntity();
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
			playerEntity.traits.with(TraitControllable.class, twc -> twc.onEachFrame());
			selectedBlock = playerEntity.traits.tryWith(TraitVoxelSelection.class,
					tvs -> tvs.getBlockLookingAt(true, false));
		}

		// Main render call
		//TODO world.getWorldRenderer().renderWorld(renderer);

		// Debug draws
		/*if (client.getConfiguration().getBooleanOption("client.debug.physicsVisualization") && playerEntity != null) {
			wireframeDebugger.render(renderer);
		}

		if (!guiHidden && selectedBlock != null)
			selectionRenderer.drawSelectionBox(renderer, selectedBlock);*/

		// Fades in & out the overlay
		if (!isCovered()) {
			if (pauseOverlayFade > 0.0)
				pauseOverlayFade -= 0.1;
		} else {
			float maxFade = 1.0f;
			if (gui.getTopLayer() instanceof ChatPanelOverlay)
				maxFade = 0.25f;
			if (pauseOverlayFade < maxFade)
				pauseOverlayFade += 0.1;
		}

		// Blit the final 3d image
		//world.getWorldRenderer().blitFinalImage(renderer, guiHidden);

		// Draw the GUI
		if (!guiHidden) {
			chatManager.render(renderer);

			// Draw inventory
			if (inventoryBarDrawer != null)
				inventoryBarDrawer.drawPlayerInventorySummary(renderer, gui.getViewportWidth() / 2 - 7, 64 + 64);

			// Draw debug worldInfo
			//if (client.getConfiguration().getBooleanValue("client.debug.showDebugInfo"))
			//	debugInfoRenderer.drawF3debugMenu(renderer);

			renderer.drawBox(gui.getViewportWidth() / 2 - 8, gui.getViewportHeight() / 2 - 8, 16, 16, "textures/gui/cursor.png");
		}

		// Lack of overlay should infer autofocus
		if (!isCovered())
			focus(true);

		// Check connection didn't died and change scene if it has
		if (world instanceof WorldClientRemote) {
			if (!((WorldClientRemote) world).getConnection().isOpen())
				client.exitToMainMenu("Connection terminated (TODO betterize)");
		}

		// Auto-switch to pause if it detects the game isn't in focus anymore
		if (!client.getGameWindow().hasFocus() && !isCovered()) {
			focus(false);
			gui.setTopLayer(new PauseMenu(gui, gui.getTopLayer()));
		}
	}

	public void focus(boolean makeInFocus) {
		if (makeInFocus && !gui.getMouse().isGrabbed()) {
			client.getInputsManager().getMouse().setGrabbed(true);
			client.getInputsManager().getMouse().setMouseCursorLocation(gui.getViewportWidth() / 2, gui.getViewportHeight() / 2);
		} else if(!makeInFocus)
			client.getInputsManager().getMouse().setGrabbed(false);
	}

	@Override
	public boolean handleInput(Input input) {
		Entity playerEntity = player.getControlledEntity();
		// Block inputs if chatting
		if (input.equals("chat")) {
			gui.setTopLayer(chatManager.new ChatPanelOverlay(gui, this));
			focus(false);
			guiHidden = false;
			return true;
		} else if (input.equals("hideGui")) {
			guiHidden = !guiHidden;
			return true;
		} else if (input.equals("screenshot")) {
			client.getGameWindow().takeScreenshot();
		} else if (input.equals("toggleDebugInfo")) {
			OptionBoolean debugInfo = client.getConfiguration().get("client.debug.showDebugInfo");
			debugInfo.toggle();
			guiHidden = false;
			return true;
		} else if (input.equals("takeCubemap")) {
			// shouldTakeACubemap = true;
			return true;
			// CTRL-F12 reloads
		} else if (input.equals("reloadContent")) {
			// Rebuild the mod FS
			client.getClient().reloadAssets();

			// Reload plugins
			world.getPluginManager().reloadPlugins();

			// Mark some caches dirty
			//TODO world.getWorldRenderer().reloadContentSpecificStuff();
			return true;
			// CTRL-R redraws chunks
		} else if (input.equals("redrawChunks")) {
			//TODO
			//((ClientParticlesRenderer) world.getParticlesManager()).cleanAllParticles();
			//world.redrawEverything();
			//world.getWorldRenderer().flagChunksModified();
			return true;
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
			gui.setTopLayer(new PauseMenu(gui, this));
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
