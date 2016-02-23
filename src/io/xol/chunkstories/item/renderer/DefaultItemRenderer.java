package io.xol.chunkstories.item.renderer;

import io.xol.chunkstories.item.Item;
import io.xol.chunkstories.item.ItemPile;
import io.xol.engine.model.RenderingContext;

import org.lwjgl.util.vector.Matrix4f;

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderItemInWorld(RenderingContext context, ItemPile pile, Matrix4f handTransformation)
	{
		// TODO Auto-generated method stub
		
	}

}
