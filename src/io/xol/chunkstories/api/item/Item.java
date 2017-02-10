package io.xol.chunkstories.api.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Item
{
	private final ItemType type;
	protected ItemRenderer itemRenderer;
	
	public Item(ItemType type)
	{
		this.type = type;
		itemRenderer = new DefaultItemRenderer(this);
	}
	
	public ItemType getType()
	{
		return type;
	}
	
	public String getName()
	{
		return type.getInternalName();
	}
	
	public ItemRenderer getItemRenderer()
	{
		return itemRenderer;
	}
	
	/**
	 * Should be called when the owner has this item selected
	 * @param owner
	 */
	public void tickInHand(WorldAuthority authority, Entity owner, ItemPile itemPile)
	{
		
	}
	
	/**
	 * Handles some input from the user
	 * @param user
	 * @param pile
	 * @param input
	 * @return false if the item doesn't handle the input, true if it does
	 */
	public boolean handleInteraction(Entity owner, ItemPile itemPile, Input input, Controller controller)
	{
		return false;
	}
	
	/**
	 * Use : determine if two stacks can be merged together, should be overriden when items have extra info.
	 * @return Returns true if the two items are similar and can share a stack without loosing information.
	 */
	public boolean canMergeWith(Item item)
	{
		return type.equals(item.getType());
	}
	
	/**
	 * For Items not implementing a custom renderer, it just shows a dull icon and thus require an icon texture.
	 * @return The full path to the image file.
	 */
	public String getTextureName(ItemPile pile)
	{
		return "./items/icons/"+getInternalName()+".png";
	}
	
	/**
	 * Returns the assignated ID for this item.
	 * @return
	 */
	public final int getID()
	{
		return type.getID();
	}
	
	// ----- Begin get/set hell -----
	
	public String getInternalName()
	{
		return type.getInternalName();
	}

	/**
	 * Called on loading an ItemPile containing this item, usefull for loading stuff into the itemData of the pile.
	 * @param stream
	 * @throws IOException
	 */
	public void load(DataInputStream stream) throws IOException
	{
		
	}
	
	/**
	 * See load()
	 * @param stream
	 * @throws IOException
	 */
	public void save(DataOutputStream stream) throws IOException
	{
		
	}
	// ----- End get/set hell -----
}
