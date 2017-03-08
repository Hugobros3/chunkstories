package io.xol.chunkstories.item.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class InventoryAllVoxels extends BasicInventory
{
	public InventoryAllVoxels()
	{
		super(0, 0);
		List<ItemPile> allItems = new ArrayList<ItemPile>();
		Set<Integer> allIds = VoxelsStore.get().getAllLoadedVoxelIds();
		for(int id : allIds)
		{
			Voxel vox = VoxelsStore.get().getVoxelById(id);
			for(ItemPile item : vox.getItems())
			{
				allItems.add(item);
			}
		}
		this.height = (int)Math.ceil(allItems.size() / 10.0);
		this.width = 10;
		this.contents = new ItemPile[width][height];
		//this.addItemPile(new ItemPile("weapon_ak47"));
		
		for(ItemPile pile : allItems)
		{
			pile.setAmount(pile.getItem().getType().getMaxStackSize());
			this.addItemPile(pile);
		}
	}
	
	@Override
	public String getInventoryName()
	{
		return "All voxels";
	}
}
