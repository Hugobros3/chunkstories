//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui;

import xyz.chunkstories.api.entity.traits.serializable.TraitInventory;
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem;
import xyz.chunkstories.api.gui.GuiDrawer;
import xyz.chunkstories.api.input.Mouse;
import xyz.chunkstories.api.item.inventory.Inventory;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.gui.layer.ingame.InventoryView;
import org.joml.Vector4f;


/**
 * Helps with rendering the inventory grid
 */
//TODO refactor into a neater component with width and shit
public class InventoryGridRenderer {
    private Inventory inventory;

    public InventoryGridRenderer(Inventory entityInventories) {
        this.inventory = entityInventories;
    }

    private int[] selectedSlot;
    private boolean closedButton = false;

    public int[] getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isOverCloseButton() {
        return closedButton;
    }

    public void drawPlayerInventorySummary(GuiDrawer drawer, int x, int y) {
        int selectedSlot = -1;
        if (inventory instanceof TraitInventory) {
            TraitSelectedItem esi = ((TraitInventory) inventory).entity.traits.get(TraitSelectedItem.class);
            if (esi != null)
                selectedSlot = esi.getSelectedSlot();
        }

        drawInventory(drawer, x - slotsWidth(getInventory().getWidth()) / 2, y /*- slotsHeight(getInventory().getHeight(), true, 0) / 2*/, true, 0, selectedSlot);
    }

    //TODO move to own layer
    public void drawInventory(GuiDrawer drawer, int x, int y, boolean summary, int blankLines, int highlightSlot) {
        Mouse mouse = drawer.getGui().getMouse();
        if (getInventory() == null)
            return;

        int scale = 1;

        int cornerSize = 8 * scale;
        int internalWidth = getInventory().getWidth() * 24 * scale;

        int height = summary ? 1 : getInventory().getHeight();

        int internalHeight = (height + (summary ? 0 : 1) + blankLines) * 24 * scale;
        int slotSize = 24 * scale;

        String inventoryTexture = "./textures/gui/inventory/inventory.png";

        Vector4f color = new Vector4f(1f, 1f, 1f, summary ? 0.5f : 1f);
        // All 8 corners
        drawer.drawBox(x, y + internalHeight + cornerSize, cornerSize,
                cornerSize, 0, 0.03125f, 0.03125f, 0, inventoryTexture, color);
        drawer.drawBox(x + cornerSize, y + internalHeight + cornerSize,
                internalWidth, cornerSize, 0.03125f, 0.03125f, 0.96875f, 0, inventoryTexture, color);
        drawer.drawBox(x + cornerSize + internalWidth,
                y + internalHeight + cornerSize, cornerSize, cornerSize, 0.96875f, 0.03125f, 1f, 0, inventoryTexture,
                color);
        drawer.drawBox(x, y, cornerSize, cornerSize, 0, 1f, 0.03125f, 248 / 256f,
                inventoryTexture, color);
        drawer.drawBox(x + cornerSize, y, internalWidth, cornerSize, 0.03125f,
                1f, 0.96875f, 248 / 256f, inventoryTexture, color);
        drawer.drawBox(x + cornerSize + internalWidth, y, cornerSize, cornerSize,
                0.96875f, 1f, 1f, 248 / 256f, inventoryTexture, color);
        drawer.drawBox(x, y + cornerSize, cornerSize, internalHeight, 0,
                248f / 256f, 0.03125f, 8f / 256f, inventoryTexture, color);
        drawer.drawBox(x + cornerSize + internalWidth, y + cornerSize,
                cornerSize, internalHeight, 248 / 256f, 248f / 256f, 1f, 8f / 256f, inventoryTexture, color);
        // Actual inventory slots
        int sumSlots2HL = 0;
        selectedSlot = null;
        for (int i = 0; i < getInventory().getWidth(); i++) {
            for (int j = 0; j < height; j++) {
                boolean mouseOver = mouse.getCursorX() > x + cornerSize + i * slotSize
                        && mouse.getCursorX() <= x + cornerSize + i * slotSize + slotSize
                        && mouse.getCursorY() > y + cornerSize + j * slotSize
                        && mouse.getCursorY() <= y + cornerSize + j * slotSize + slotSize;
                // Just a dirt hack to always keep selected slot values where we want them
                if (mouseOver && selectedSlot == null) {
                    selectedSlot = new int[]{i, j};
                }

                ItemPile selectedPile = null;
                if (selectedSlot != null)
                    selectedPile = getInventory().getItemPileAt(selectedSlot[0], selectedSlot[1]);
                ItemPile thisPile = getInventory().getItemPileAt(i, j);

                if (summary) {
                    ItemPile summaryBarSelected = getInventory().getItemPileAt(highlightSlot, 0);
                    if (summaryBarSelected != null && i == summaryBarSelected.getX()) {
                        sumSlots2HL = summaryBarSelected.getItem().getDefinition().getSlotsWidth();
                    }
                    if (sumSlots2HL > 0 || summaryBarSelected == null && highlightSlot == i) {
                        sumSlots2HL--;
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
                                152 / 256f, inventoryTexture, color);
                    } else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
                                152 / 256f, inventoryTexture, color);

                } else {
                    if (mouseOver || selectedPile != null && thisPile != null && selectedPile.getX() == thisPile.getX()
                            && selectedPile.getY() == thisPile.getY()) {
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
                                152 / 256f, inventoryTexture, color);
                    } else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
                                152 / 256f, inventoryTexture, color);

                }
            }
        }

        // Blank part ( usefull for special inventories, ie player )
        for (int j = getInventory().getHeight(); j < getInventory().getHeight() + blankLines; j++) {
            for (int i = 0; i < getInventory().getWidth(); i++) {
                if (j == getInventory().getHeight()) {
                    if (i == getInventory().getWidth() - 1)
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 152 / 256f, 248 / 256f,
                                128 / 256f, inventoryTexture, color);
                    else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 152 / 256f, 32f / 256f,
                                128 / 256f, inventoryTexture, color);
                } else {
                    if (i == getInventory().getWidth() - 1)
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 56 / 256f, 248 / 256f,
                                32 / 256f, inventoryTexture, color);
                    else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 56 / 256f, 32f / 256f,
                                32 / 256f, inventoryTexture, color);
                }
            }
        }
        // Top part
        if (!summary) {
            drawer.drawBox(x + cornerSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 8f / 256f, 32f / 256f, 32f / 256f,
                    8f / 256f, inventoryTexture, color);

            for (int i = 1; i < getInventory().getWidth() - 2; i++) {
                drawer.drawBox(x + cornerSize + i * slotSize,
                        y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 32f / 256f, 32f / 256f,
                        56f / 256f, 8f / 256f, inventoryTexture, color);
            }
            drawer.drawBox(
                    x + cornerSize + (getInventory().getWidth() - 2) * slotSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 200f / 256f, 32f / 256f, 224 / 256f,
                    8f / 256f, inventoryTexture, color);
            closedButton = mouse.getCursorX() > x + cornerSize + (getInventory().getWidth() - 1) * slotSize
                    && mouse.getCursorX() <= x + cornerSize + (getInventory().getWidth() - 1) * slotSize + slotSize
                    && mouse.getCursorY() > y + cornerSize + internalHeight - slotSize
                    && mouse.getCursorY() <= y + cornerSize + internalHeight;

            drawer.drawBox(
                    x + cornerSize + (getInventory().getWidth() - 1) * slotSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 224f / 256f, 32f / 256f,
                    248f / 256f, 8f / 256f, inventoryTexture, color);

            drawer.drawStringWithShadow(
                    drawer.getFonts().getFont("LiberationSans-Bold", 12), x + cornerSize + 6,
                    y + cornerSize + internalHeight - slotSize + 2 * scale, getInventory().getInventoryName(), -1, new Vector4f(1, 1, 1, 1));
        }


        // Draw the actual items
        for (ItemPile pile : getInventory()) {
            int i = pile.getX();
            int j = pile.getY();
            if (pile != null && (!summary || j == 0)) {
                int center = summary ? slotSize * (pile.getItem().getDefinition().getSlotsHeight() - 1) / 2 : 0;

                // TODO just icons now.
                //pile.getItem().getDefinition().getRenderer().renderItemInInventory(renderer, pile,
                //        x + cornerSize + i * slotSize, y - center + cornerSize + j * slotSize, scale);
            }
        }

        // Draws the item's text ( done later to allow fontRenderer to pool their draws )
        for (ItemPile pile : getInventory()) {
            int i = pile.getX();
            int j = pile.getY();

            if (!summary || j == 0) {
                int amountToDisplay = pile.getAmount();

                // If we selected this item
                if (InventoryView.Companion.getDraggingPile() != null && InventoryView.Companion.getDraggingPile().getInventory() != null
                        && getInventory().equals(InventoryView.Companion.getDraggingPile().getInventory())
                        && InventoryView.Companion.getDraggingPile().getX() == i && InventoryView.Companion.getDraggingPile().getY() == j) {
                    amountToDisplay -= InventoryView.Companion.getDraggingQuantity();
                }

                // Draw amount of items in the pile
                if (amountToDisplay > 1)
                    drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
                            x + cornerSize + (pile.getItem().getDefinition().getSlotsWidth() - 1 + i) * slotSize,
                            y + cornerSize + j * slotSize, amountToDisplay + "", -1,
                            new Vector4f(1, 1, 1, 1));
            }
        }
    }

    public int slotsWidth(int slots) {
        return 8 + slots * 24;
    }

    public int slotsHeight(int slots, boolean summary, int blankLines) {
        return 8 + (slots + (summary ? 0 : 1) + blankLines) * 24;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
