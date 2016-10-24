package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;

public class VoxelMeta16 extends VoxelDefault
{
	VoxelTexture colors[] = new VoxelTexture[16];

	public VoxelMeta16(int id, String name)
	{
		super(id, name);
		for (int i = 0; i < 16; i++)
			colors[i] = VoxelTextures.getVoxelTexture(name + "." + i);
	}

	@Override
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info) // 0 for top, 1 bot,
	// 2,3,4,5
	// north/south/east/west
	{
		int meta = VoxelFormat.meta(data);
		// System.out.println("swag");
		return colors[meta];
	}
	
	@Override
	public ItemPile[] getItems()
	{
		ItemPile[] items = new ItemPile[16];
		for(int i = 0; i < 16; i++)
		{
			ItemVoxel itemVoxel = (ItemVoxel)ItemTypes.getItemTypeByName("item_voxel").newItem();
			itemVoxel.voxel = this;
			itemVoxel.voxelMeta = i;			
			
			items[i] = new ItemPile(itemVoxel);
		};
		return items;
	}
}
