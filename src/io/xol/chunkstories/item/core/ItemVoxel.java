package io.xol.chunkstories.item.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.actions.ClientAction;
import io.xol.chunkstories.api.events.actions.ClientActionMouseClick;
import io.xol.chunkstories.api.events.actions.ClientActionMouseClick.MouseButton;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.item.ItemData;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.VoxelItemRenderer;
import io.xol.chunkstories.voxel.VoxelTypes;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * An item that contains voxels
 */
public class ItemVoxel extends Item
{
	class ItemDataVoxel implements ItemData
	{
		public Voxel voxel = null;
		public int voxelMeta = 0;
	}

	public ItemVoxel(int id)
	{
		super(id);
		itemRenderer = new VoxelItemRenderer(this);
	}

	public ItemData getItemData()
	{
		return new ItemDataVoxel();
	}

	public void onCreate(ItemPile pile, String[] info)
	{
		ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (info != null && info.length > 0)
			idv.voxel = VoxelTypes.get(Integer.parseInt(info[0]));
		if (info != null && info.length > 1)
			idv.voxelMeta = Integer.parseInt(info[1]) % 16;
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		ItemDataVoxel idv = (ItemDataVoxel) pile.data;
		if (idv.voxel != null)
			return "res/voxels/textures/" + idv.voxel.getName() + ".png";
		return "res/items/icons/notex.png";
	}

	public Voxel getVoxel(ItemPile pile)
	{
		return ((ItemDataVoxel) pile.getData()).voxel;
	}

	public int getVoxelMeta(ItemPile pile)
	{
		return ((ItemDataVoxel) pile.getData()).voxelMeta;
	}

	public void onUse(Entity user, ItemPile pile, ClientAction action)
	{
		if (action instanceof ClientActionMouseClick)
		{
			EntityPlayer player = (EntityPlayer) user;
			ClientActionMouseClick mouseClick = (ClientActionMouseClick) action;
			int voxelID = ((ItemDataVoxel)pile.getData()).voxel.getId();
			int voxelMeta = ((ItemDataVoxel)pile.getData()).voxelMeta;
			if (mouseClick.getMouseButtonPressed() == MouseButton.MOUSE_RIGHT)
			{
				int[] selectedBlock = player.rayTraceSelectedBlock(false);
				if (selectedBlock != null)
				{
					user.getWorld().setDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2], VoxelFormat.format(voxelID, voxelMeta, 0, 0), true);
					//worldRenderer.modified();
				}
			}
			else if (mouseClick.getMouseButtonPressed() == MouseButton.MOUSE_LEFT)
			{
				int[] selectedBlock = player.rayTraceSelectedBlock(true);
				if (selectedBlock != null)
				{
					user.getWorld().setDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2], 0, true);
					//worldRenderer.modified();
				}
			}
			else if (mouseClick.getMouseButtonPressed() == MouseButton.MOUSE_MIDDLE)
			{
				int[] selectedBlock = player.rayTraceSelectedBlock(true);
				if (selectedBlock != null)
				{
					int data = user.getWorld().getDataAt(selectedBlock[0], selectedBlock[1], selectedBlock[2]);
					
					((ItemDataVoxel)pile.getData()).voxel = VoxelTypes.get(VoxelFormat.id(data));
					((ItemDataVoxel)pile.getData()).voxelMeta = VoxelFormat.meta(data);
					//voxelId = VoxelFormat.id(data);
					//meta = VoxelFormat.meta(data);
				}
			}
		}
	}

	@Override
	public void load(ItemPile itemPile, DataInputStream stream) throws IOException
	{
		((ItemDataVoxel) itemPile.data).voxel = VoxelTypes.get(stream.readInt());
		((ItemDataVoxel) itemPile.data).voxelMeta = (int) stream.readByte();
	}

	@Override
	public void save(ItemPile itemPile, DataOutputStream stream) throws IOException
	{
		//System.out.println(itemPile.item);
		//System.out.println(itemPile.data);
		stream.writeInt(((ItemDataVoxel) itemPile.data).voxel.getId());
		stream.writeByte((byte) ((ItemDataVoxel) itemPile.data).voxelMeta);
	}

}
