package io.xol.chunkstories.api.item;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Provides an interface to render itemPiles
 * @author gobrosse
 *
 */
public interface ItemRenderer
{
	/**
	 * Renders the item for the 2D inventory overlay
	 * @param context
	 * @param pile
	 * @param screenPositionX
	 * @param screenPositionY
	 * @param scaling
	 */
	public void renderItemInInventory(RenderingContext context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling);
	
	/**
	 * Renders the item in the hand of the playing entity (or wherever the entity model is shown holding items)
	 * @param context
	 * @param pile
	 * @param handTransformation Can be modified
	 */
	public void renderItemInWorld(RenderingContext context, ItemPile pile, World world, Location location, Matrix4f handTransformation);
}
