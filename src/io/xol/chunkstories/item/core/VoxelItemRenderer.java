package io.xol.chunkstories.item.core;

import org.lwjgl.util.vector.Matrix4f;

import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.ItemRenderer;
import io.xol.engine.model.RenderingContext;

public class VoxelItemRenderer implements ItemRenderer
{
	ItemVoxel itemVoxel;
	Matrix4f transformation = new Matrix4f();
	
	public VoxelItemRenderer(ItemVoxel itemVoxel)
	{
		
	}

	@Override
	public void renderItemInInventory(RenderingContext context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		/*int slotSize = 24 * 2;
		TexturesHandler.getTexture(pile.getTextureName()).setLinearFiltering(false);
		int textureId = TexturesHandler.getTexture(pile.getTextureName()).getID();
		if(textureId == -1)
			textureId = TexturesHandler.getTexture("res/items/icons/notex.png").getID();
		int width = slotSize * pile.item.getSlotsWidth();
		int height = slotSize * pile.item.getSlotsHeight();
		GuiDrawer.drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, textureId, true, true, null);*/
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, Matrix4f handTransformation)
	{
		// TODO Auto-generated method stub

	}

}
