package io.xol.chunkstories.item.renderer;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemRenderer;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefaultItemRenderer implements ItemRenderer
{
	Item item;
	
	public DefaultItemRenderer(Item item)
	{
		this.item = item;
	}

	@Override
	public void renderItemInInventory(RenderingContext context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		int slotSize = 24 * 2;
		TexturesHandler.getTexture(pile.getTextureName()).setLinearFiltering(false);
		int textureId = TexturesHandler.getTextureID(pile.getTextureName());
		if(textureId == -1)
			textureId = TexturesHandler.getTexture("res/items/icons/notex.png").getId();
		int width = slotSize * pile.item.getSlotsWidth();
		int height = slotSize * pile.item.getSlotsHeight();
		context.getGuiRenderer().drawBoxWindowsSpaceWithSize(screenPositionX, screenPositionY, width, height, 0, 1, 1, 0, textureId, true, true, null);
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, World world, Matrix4f handTransformation)
	{
		
	}

}
