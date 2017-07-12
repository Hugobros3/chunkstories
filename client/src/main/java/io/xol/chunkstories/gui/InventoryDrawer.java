package io.xol.chunkstories.gui;

import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.core.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryDrawer
{
	private Inventory inventory;
	private EntityWithSelectedItem entity;

	public InventoryDrawer(Inventory entityInventories)
	{
		this.inventory = entityInventories;
	}

	public InventoryDrawer(EntityWithSelectedItem entity)
	{
		this.entity = entity;
	}

	public void drawInventoryCentered(RenderingInterface context, int x, int y, int scale, boolean summary, int blankLines)
	{
		drawInventory(context, x - slotsWidth(getInventory().getWidth(), scale) / 2, y - slotsHeight(getInventory().getHeight(), scale, summary, blankLines) / 2, scale, summary, blankLines, -1);
	}

	int[] selectedSlot;
	boolean closedButton = false;

	public int[] getSelectedSlot()
	{
		return selectedSlot;
	}

	public boolean isOverCloseButton()
	{
		return closedButton;
	}

	public void drawPlayerInventorySummary(RenderingInterface renderingContext, int x, int y)
	{
		//Don't draw inventory only
		if (entity == null)
			return;
		drawInventory(renderingContext, x - slotsWidth(getInventory().getWidth(), 2) / 2, y - slotsHeight(getInventory().getHeight(), 2, true, 0) / 2, 2, true, 0, entity.getSelectedItemComponent().getSelectedSlot());
	}

	public void drawInventory(RenderingInterface context, int x, int y, int scale, boolean summary, int blankLines, int highlightSlot)
	{
		Mouse mouse = context.getClient().getInputsManager().getMouse();
		if (getInventory() == null)
			return;

		int cornerSize = 8 * scale;
		int internalWidth = getInventory().getWidth() * 24 * scale;

		int height = summary ? 1 : getInventory().getHeight();

		int internalHeight = (height + (summary ? 0 : 1) + blankLines) * 24 * scale;
		int slotSize = 24 * scale;

		Texture2D inventoryTexture = TexturesHandler.getTexture("./textures/gui/inventory/inventory.png");
		inventoryTexture.setLinearFiltering(false);

		Vector4f color = new Vector4f(1f, 1f, 1f, summary ? 0.5f : 1f);
		//All 8 corners
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y + internalHeight + cornerSize, cornerSize, cornerSize, 0, 0.03125f, 0.03125f, 0, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize, y + internalHeight + cornerSize, internalWidth, cornerSize, 0.03125f, 0.03125f, 0.96875f, 0, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth, y + internalHeight + cornerSize, cornerSize, cornerSize, 0.96875f, 0.03125f, 1f, 0, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y, cornerSize, cornerSize, 0, 1f, 0.03125f, 248 / 256f, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize, y, internalWidth, cornerSize, 0.03125f, 1f, 0.96875f, 248 / 256f, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth, y, cornerSize, cornerSize, 0.96875f, 1f, 1f, 248 / 256f, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x, y + cornerSize, cornerSize, internalHeight, 0, 248f / 256f, 0.03125f, 8f / 256f, inventoryTexture, true, true, color);
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + internalWidth, y + cornerSize, cornerSize, internalHeight, 248 / 256f, 248f / 256f, 1f, 8f / 256f, inventoryTexture, true, true, color);
		//Actual inventory slots
		int sumSlots2HL = 0;
		boolean foundTheVegan = false;
		for (int i = 0; i < getInventory().getWidth(); i++)
		{
			for (int j = 0; j < height; j++)
			{
				boolean mouseOver = mouse.getCursorX() > x + cornerSize + i * slotSize && mouse.getCursorX() <= x + cornerSize + i * slotSize + slotSize && mouse.getCursorY() > y + cornerSize + j * slotSize && mouse.getCursorY() <= y + cornerSize + j * slotSize + slotSize;
				//Just a dirt hack to always keep selecte slot values where we want them
				if (mouseOver)
				{
					selectedSlot = new int[] { i, j };
					foundTheVegan = true;
				}

				ItemPile selectedPile = null;
				if (selectedSlot != null)
					selectedPile = getInventory().getItemPileAt(selectedSlot[0], selectedSlot[1]);
				ItemPile thisPile = getInventory().getItemPileAt(i, j);

				if (summary)
				{
					ItemPile summaryBarSelected = getInventory().getItemPileAt(highlightSlot, 0);
					if (summaryBarSelected != null && i == summaryBarSelected.getX())
					{
						sumSlots2HL = summaryBarSelected.getItem().getType().getSlotsWidth();
					}
					if (sumSlots2HL > 0 || (summaryBarSelected == null && highlightSlot == i))
					{
						sumSlots2HL--;
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f, 152 / 256f, inventoryTexture, true, true, color);
					}
					else
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f, 152 / 256f, inventoryTexture, true, true, color);

				}
				else
				{
					if (mouseOver || (selectedPile != null && thisPile != null && selectedPile.getX() == thisPile.getX() && selectedPile.getY() == thisPile.getY()))
					{
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f, 152 / 256f, inventoryTexture, true, true, color);
					}
					else
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f, 152 / 256f, inventoryTexture, true, true, color);

				}
			}
		}
		if (!foundTheVegan)
			selectedSlot = null;
		//Blank part ( usefull for special inventories, ie player )
		for (int j = getInventory().getHeight(); j < getInventory().getHeight() + blankLines; j++)
		{
			for (int i = 0; i < getInventory().getWidth(); i++)
			{
				if (j == getInventory().getHeight())
				{
					if (i == getInventory().getWidth() - 1)
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 152 / 256f, 248 / 256f, 128 / 256f, inventoryTexture, true, true, color);
					else
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 152 / 256f, 32f / 256f, 128 / 256f, inventoryTexture, true, true, color);
				}
				else
				{
					if (i == getInventory().getWidth() - 1)
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 56 / 256f, 248 / 256f, 32 / 256f, inventoryTexture, true, true, color);
					else
						context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 56 / 256f, 32f / 256f, 32 / 256f, inventoryTexture, true, true, color);
				}
			}
		}
		//Top part
		if (!summary)
		{
			context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize, y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 8f / 256f, 32f / 256f, 32f / 256f, 8f / 256f, inventoryTexture, true, true, color);
			for (int i = 1; i < getInventory().getWidth() - 2; i++)
			{
				context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + i * slotSize, y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 32f / 256f, 32f / 256f, 56f / 256f, 8f / 256f, inventoryTexture, true, true, color);
			}
			context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + (getInventory().getWidth() - 2) * slotSize, y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 200f / 256f, 32f / 256f, 224 / 256f, 8f / 256f,
					inventoryTexture, true, true, color);
			closedButton = mouse.getCursorX() > x + cornerSize + (getInventory().getWidth() - 1) * slotSize && mouse.getCursorX() <= x + cornerSize + (getInventory().getWidth() - 1) * slotSize + slotSize
					&& mouse.getCursorY() > y + cornerSize + internalHeight - slotSize && mouse.getCursorY() <= y + cornerSize + internalHeight;

			context.getGuiRenderer().drawBoxWindowsSpaceWithSize(x + cornerSize + (getInventory().getWidth() - 1) * slotSize, y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 224f / 256f, 32f / 256f, 248f / 256f, 8f / 256f,
					inventoryTexture, true, true, color);
		}

		//Get rid of any remaining GUI elements or else they will draw on top of the items
		context.getGuiRenderer().drawBuffer();

		//Draw the actual items
		for (ItemPile pile : getInventory())
		{
			int i = pile.getX();
			int j = pile.getY();
			if (pile != null && (!summary || j == 0))
			{
				int center = summary ? slotSize * (pile.getItem().getType().getSlotsHeight() - 1) / 2 : 0;
				pile.getItem().getType().getRenderer().renderItemInInventory(context, pile, x + cornerSize + i * slotSize, y - center + cornerSize + j * slotSize, scale);
			}
		}

		//Draws the item's text ( done later to allow gpu commands merging )
		for (ItemPile pile : getInventory())
		{
			int i = pile.getX();
			int j = pile.getY();

			if (pile != null && (!summary || j == 0))
			{
				int amountToDisplay = pile.getAmount();
				//If we selected this item
				if ((InventoryOverlay.selectedItem != null && InventoryOverlay.selectedItem.getInventory() != null && getInventory().equals(InventoryOverlay.selectedItem.getInventory()) && InventoryOverlay.selectedItem.getX() == i
						&& InventoryOverlay.selectedItem.getY() == j))
				{
					amountToDisplay -= InventoryOverlay.selectedItemAmount;
				}

				if (amountToDisplay > 1)
					context.getFontRenderer().drawStringWithShadow(context.getFontRenderer().defaultFont(), x + cornerSize + ((pile.getItem().getType().getSlotsWidth() - 1.0f) + i) * slotSize, y + cornerSize + j * slotSize, amountToDisplay + "", scale, scale,
							new Vector4f(1, 1, 1, 1));
			}
		}
	}

	public int slotsWidth(int slots, int scale)
	{
		return (8 + slots * 24) * scale;
	}

	public int slotsHeight(int slots, int scale, boolean summary, int blankLines)
	{
		return (8 + (slots + (summary ? 0 : 1) + blankLines) * 24) * scale;
	}

	public Inventory getInventory()
	{
		if (entity == null)
			return inventory;
		return entity.getInventory();
	}
}
