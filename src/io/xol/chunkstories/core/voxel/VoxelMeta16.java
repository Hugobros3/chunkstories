package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelTextureAtlased;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelMeta16 extends Voxel
{
	VoxelTextureAtlased colors[] = new VoxelTextureAtlased[16];

	public VoxelMeta16(VoxelType type)
	{
		super(type);
		for (int i = 0; i < 16; i++)
			colors[i] = store.textures().getVoxelTextureByName(getName() + "." + i);
	}

	@Override
	public VoxelTextureAtlased getVoxelTexture(int data, VoxelSides side, VoxelContext info) // 0 for top, 1 bot,
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
			ItemVoxel itemVoxel = (ItemVoxel)store.parent().items().getItemTypeByName("item_voxel").newItem();
			itemVoxel.voxel = this;
			itemVoxel.voxelMeta = i;			
			
			items[i] = new ItemPile(itemVoxel);
		};
		return items;
	}
}
