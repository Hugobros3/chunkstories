package io.xol.chunkstories.api.item;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Renders items
 */
public class ItemRenderer
{
	protected final ItemRenderer fallbackRenderer;
	
	public ItemRenderer(ItemRenderer fallbackRenderer)
	{
		this.fallbackRenderer = fallbackRenderer;
	}

	/**
	 * Renders the item for the 2D inventory overlay
	 * @param context
	 * @param pile
	 * @param screenPositionX
	 * @param screenPositionY
	 * @param scaling
	 */
	public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		fallbackRenderer.renderItemInInventory(renderingInterface, pile, screenPositionX, screenPositionY, scaling);
	}

	/**
	 * Renders the item in the hand of the playing entity (or wherever the entity model is shown holding items)
	 * @param renderingContext
	 * @param pile
	 * @param handTransformation Can be modified
	 */
	public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world, Location location, Matrix4f transformation)
	{
		fallbackRenderer.renderItemInWorld(renderingInterface, pile, world, location, transformation);
	}
}
