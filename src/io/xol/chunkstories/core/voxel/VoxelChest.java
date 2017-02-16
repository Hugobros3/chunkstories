package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.voxel.EntityChest;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelChest extends VoxelEntity
{
	VoxelTexture frontTexture;
	VoxelTexture sideTexture;
	VoxelTexture topTexture;
	
	public VoxelChest(VoxelType type)
	{
		super(type);
		
		frontTexture = store.textures().getVoxelTextureByName(getName() + "front");
		sideTexture = store.textures().getVoxelTextureByName(getName() + "side");
		topTexture = store.textures().getVoxelTextureByName(getName() + "top");
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input, int voxelData)
	{
		//Open GUI
		if(input.getName().equals("mouse.right") && entity.getWorld() instanceof WorldClient)
		{	
			Client.getInstance().openInventory(((EntityChest)this.getVoxelEntity(voxelLocation)).getInventory());
			return true;
		}
		return false;
	}

	@Override
	public EntityChest getVoxelEntity(World world, int worldX, int worldY, int worldZ)
	{
		EntityVoxel ev = super.getVoxelEntity(world, worldX, worldY, worldZ);
		if(!(ev instanceof EntityChest))
				throw new RuntimeException("VoxelEntity representation invariant fail, wrong entity found at " + worldX + ":" + worldY + ":" + worldZ);
		return (EntityChest) ev;
	}

	@Override
	protected EntityVoxel createVoxelEntity(World world, int x, int y, int z)
	{
		return new EntityChest((WorldImplementation) world, x, y, z);
	}
	
	@Override
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info)
	{
		VoxelSides actualSide = VoxelSides.getSideMcStairsChestFurnace(VoxelFormat.meta(data));
		
		if(side.equals(VoxelSides.TOP))
			return topTexture;
		
		if(side.equals(actualSide))
			return frontTexture;
		
		return sideTexture;
	}
	
	@Override
	//Chunk stories chests use Minecraft format to ease porting of maps
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		super.onPlace(world, x, y, z, voxelData, entity);
		
		int stairsSide = 0;
		//See: 
		//http://minecraft.gamepedia.com/Data_values#Ladders.2C_Furnaces.2C_Chests.2C_Trapped_Chests
		if (entity != null)
		{
			Location loc = entity.getLocation();
			double dx = loc.getX() - (x + 0.5);
			double dz = loc.getZ() - (z + 0.5);
			if (Math.abs(dx) > Math.abs(dz))
			{
				if(dx > 0)
					stairsSide = 4;
				else
					stairsSide = 5;
			}
			else
			{
				if(dz > 0)
					stairsSide = 2;
				else
					stairsSide = 3;
			}
			voxelData = VoxelFormat.changeMeta(voxelData, stairsSide);
		}
		return voxelData;
	}
}
