//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import org.joml.Vector4f;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.item.EventItemDroppedToWorld;
import io.xol.chunkstories.api.events.player.PlayerMoveItemEvent;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.packets.PacketInventoryMoveItemPile;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.gui.InventoryGridRenderer;

/** GUI code that handles seeing and manipulating ItemPiles in an Inventory */
public class InventoryView extends Layer {
	private Inventory[] inventories;
	private InventoryGridRenderer[] drawers;

	public static ItemPile selectedItem;
	public static int selectedItemAmount;

	public InventoryView(GameWindow scene, Layer parent, Inventory[] entityInventories) {
		super(scene, parent);
		this.inventories = entityInventories;
		this.drawers = new InventoryGridRenderer[entityInventories.length];
		for (int i = 0; i < drawers.length; i++)
			drawers[i] = new InventoryGridRenderer(entityInventories[i]);
	}

	@Override
	public void render(RenderingInterface renderer) {
		parentLayer.render(renderer);

		Mouse mouse = renderer.getClient().getInputsManager().getMouse();

		int totalWidth = 0;
		for (Inventory inv : inventories)
			totalWidth += 2 + inv.getWidth();
		totalWidth -= 2;
		int widthAccumulation = 0;
		for (int i = 0; i < drawers.length; i++) {
			int thisWidth = inventories[i].getWidth();
			drawers[i].drawInventoryCentered(renderer,
					renderer.getWindow().getWidth() / 2 - totalWidth * 24 + thisWidth * 24 + widthAccumulation * 48,
					renderer.getWindow().getHeight() / 2, 2, false, 4 - i * 4);
			widthAccumulation += 1 + thisWidth;

			int[] highlightedSlot = drawers[i].getSelectedSlot();
			if (highlightedSlot != null) {
				ItemPile pileHighlighted = inventories[i].getItemPileAt(highlightedSlot[0], highlightedSlot[1]);
				if (pileHighlighted != null) {
					float mx = (float) mouse.getCursorX();
					float my = (float) mouse.getCursorY();

					renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), mx, my,
							pileHighlighted.getItem().getName(), 2, 2, new Vector4f(1.0f));
					// System.out.println(pileHighlighted);
				}
			}
		}

		if (selectedItem != null) {
			int slotSize = 24 * 2;

			int width = slotSize * selectedItem.getItem().getDefinition().getSlotsWidth();
			int height = slotSize * selectedItem.getItem().getDefinition().getSlotsHeight();
			selectedItem.getItem().getDefinition().getRenderer().renderItemInInventory(renderer, selectedItem,
					(float) mouse.getCursorX() - width / 2, (float) mouse.getCursorY() - height / 2, 2);

			if (selectedItemAmount != 1)
				renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
						(float) mouse.getCursorX() - width / 2
								+ (selectedItem.getItem().getDefinition().getSlotsWidth() - 1.0f) * slotSize,
						(float) mouse.getCursorY() - height / 2, selectedItemAmount + "", 2, 2,
						new Vector4f(1, 1, 1, 1));

		}
	}

	public boolean handleInput(Input input) {
		if (input instanceof MouseButton)
			return handleClick((MouseButton) input);
		else if (input.equals("exit")) {
			this.gameWindow.setLayer(parentLayer);
			InventoryView.selectedItem = null;
			return true;
		} else
			return true;// super.handleInput(input);
	}

	private boolean handleClick(MouseButton mouseButton) {
		// We assume a player has to be spawned in order to do items manipulation
		Player player = gameWindow.getClient().getPlayer();
		if (player == null) {
			this.gameWindow.setLayer(parentLayer);
			// this.mainScene.changeOverlay(parent);
			selectedItem = null;
			return true;
		}

		World world = player.getWorld();

		for (int i = 0; i < drawers.length; i++) {
			// Close button
			if (drawers[i].isOverCloseButton()) {
				this.gameWindow.setLayer(parentLayer);
				selectedItem = null;
			} else {

				int[] c = drawers[i].getSelectedSlot();
				if (c == null)
					continue;

				else {
					int x = c[0];
					int y = c[1];
					if (selectedItem == null) {
						if (mouseButton.equals("mouse.left")) {
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : selectedItem.getAmount();
						} else if (mouseButton.equals("mouse.right")) {
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : 1;
						} else if (mouseButton.equals("mouse.middle")) {
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0
									: (selectedItem.getAmount() > 1 ? selectedItem.getAmount() / 2 : 1);
						}
						// selectedItemInv = inventory;
					} else if (mouseButton.equals("mouse.right")) {
						if (selectedItem.equals(inventories[i].getItemPileAt(x, y))) {
							if (selectedItemAmount < inventories[i].getItemPileAt(x, y).getAmount())
								selectedItemAmount++;
						}
					} else if (mouseButton.equals("mouse.left")) {
						// Ignore null-sum games
						if (selectedItem.getInventory() == inventories[i] && x == selectedItem.getX()
								&& y == selectedItem.getY()) {
							selectedItem = null;
							return true;
						}

						if (world instanceof WorldMaster) {
							PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(player, selectedItem,
									selectedItem.getInventory(), inventories[i], selectedItem.getX(),
									selectedItem.getY(), x, y, selectedItemAmount);
							player.getContext().getPluginManager().fireEvent(moveItemEvent);

							// If move was successfull
							if (!moveItemEvent.isCancelled())
								selectedItem.moveItemPileTo(inventories[i], x, y, selectedItemAmount);

							selectedItem = null;
						} else if (world instanceof WorldClientNetworkedRemote) {
							// When in a remote MP scenario, send a packet
							PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(world,
									selectedItem, selectedItem.getInventory(), inventories[i], selectedItem.getX(),
									selectedItem.getY(), x, y, selectedItemAmount);
							((WorldClientNetworkedRemote) world).getRemoteServer().pushPacket(packetMove);

							// And unsellect item
							selectedItem = null;
						}
					}
					return true;
				}
			}
		}

		// Clicked outside of any other inventory (drop!)
		if (selectedItem != null) {
			// SP scenario, replicated logic in PacketInventoryMoveItemPile
			if (world instanceof WorldMaster) {
				// For local item drops, we need to make sure we have a sutiable entity
				Entity playerEntity = player.getControlledEntity();
				if (playerEntity != null) {
					PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(player, selectedItem,
							selectedItem.getInventory(), null, selectedItem.getX(), selectedItem.getY(), 0, 0,
							selectedItemAmount);
					player.getContext().getPluginManager().fireEvent(moveItemEvent);

					if (!moveItemEvent.isCancelled()) {
						// If we're pulling this out of an inventory ( and not /dev/null ), we need to
						// remove it from that
						Inventory sourceInventory = selectedItem.getInventory();

						Location loc = playerEntity.getLocation();
						EventItemDroppedToWorld dropItemEvent = new EventItemDroppedToWorld(loc, sourceInventory,
								selectedItem);
						player.getContext().getPluginManager().fireEvent(dropItemEvent);

						if (!dropItemEvent.isCancelled()) {

							if (sourceInventory != null)
								sourceInventory.setItemPileAt(selectedItem.getX(), selectedItem.getY(), null);

							if (dropItemEvent.getItemEntity() != null)
								loc.getWorld().addEntity(dropItemEvent.getItemEntity());
						}
					}
				}
				selectedItem = null;
			}
			// In MP scenario, move into /dev/null
			else if (world instanceof WorldClientNetworkedRemote) {
				PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(world, selectedItem,
						selectedItem.getInventory(), null, selectedItem.getX(), selectedItem.getY(), 0, 0,
						selectedItemAmount);
				((WorldClientNetworkedRemote) world).getRemoteServer().pushPacket(packetMove);

				selectedItem = null;
			}
		}

		return true;

	}
}
