package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.World.WorldVoxelContext;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * 2-blocks tall door Requires two consecutive voxel ids, x being lower, x+1 top, the top part should be suffixed of _top
 */
public class VoxelDoor extends Voxel implements VoxelLogic, VoxelInteractive, VoxelCustomIcon
{
	VoxelTexture doorTexture;

	VoxelModel[] models = new VoxelModel[8];

	boolean top;

	public VoxelDoor(VoxelType type)
	{
		super(type);

		top = getName().endsWith("_top");

		if (top)
			doorTexture = store.textures().getVoxelTextureByName(getName().replace("_top", "") + "_upper");
		else
			doorTexture = store.textures().getVoxelTextureByName(getName() + "_lower");

		for (int i = 0; i < 8; i++)
			models[i] = store.models().getVoxelModelByName("door.m" + i);
	}

	@Override
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info)
	{
		return doorTexture;
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int facingPassed = (info.getMetaData() >> 2) & 0x3;
		boolean isOpen = ((info.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((info.getMetaData() >> 1) & 0x1) == 1;

		int i = 0;

		if (hingeSide)
			facingPassed += 4;

		switch (facingPassed)
		{
		case 0:
			i = isOpen ? 3 : 0;
			break;
		case 1:
			i = isOpen ? 4 : 1;
			break;
		case 2:
			i = isOpen ? 5 : 6;
			break;
		case 3:
			i = isOpen ? 2 : 7;
			break;

		case 4:
			i = isOpen ? 1 : 4;
			break;
		case 5:
			i = isOpen ? 6 : 5;
			break;
		case 6:
			i = isOpen ? 7 : 2;
			break;
		case 7:
			i = isOpen ? 0 : 3;
			break;
		}

		return models[i];
	}

	//Meta
	//0x0 -> open/close
	//0x1 -> left/right hinge || left = 0 right = 1 (left is default)
	//0x2-0x4 -> side ( VoxelSide << 2 )

	@Override
	public boolean handleInteraction(Entity entity, WorldVoxelContext voxelContext, Input input)
	{
		if (!input.getName().equals("mouse.right"))
			return false;
		if (!(entity.getWorld() instanceof WorldMaster))
			return true;
		
		int voxelData = voxelContext.getData();
		boolean isOpen = ((VoxelFormat.meta(voxelData) >> 0) & 0x1) == 1;
		boolean hingeSide = ((VoxelFormat.meta(voxelData) >> 1) & 0x1) == 1;
		int facingPassed = (VoxelFormat.meta(voxelData) >> 2) & 0x3;

		boolean newState = !isOpen;

		int newData = computeMeta(newState, hingeSide, facingPassed);

		Location otherPartLocation = voxelContext.getLocation();
		if (top)
			otherPartLocation.add(0.0, -1.0, 0.0);
		else
			otherPartLocation.add(0.0, 1.0, 0.0);

		int otherLocationId = VoxelFormat.id(otherPartLocation.getVoxelDataAtLocation());
		if (VoxelsStore.get().getVoxelById(otherLocationId) instanceof VoxelDoor)
		{
			System.out.println("new door status : " + newState);
			voxelContext.getWorld().getSoundManager().playSoundEffect("sounds/sfx/door.ogg", voxelContext.getLocation(), 1.0f, 1.0f);

			voxelContext.getLocation().setVoxelDataAtLocation(VoxelFormat.changeMeta(voxelContext.getData(), newData));
			otherPartLocation.setVoxelDataAtLocation(VoxelFormat.changeMeta(otherPartLocation.getVoxelDataAtLocation(), newData));
		}
		else
		{
			ChunkStoriesLogger.getInstance().error("Incomplete door @ " + otherPartLocation);
		}

		return true;
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		CollisionBox[] boxes = new CollisionBox[1];

		int facingPassed = (info.getMetaData() >> 2) & 0x3;
		boolean isOpen = ((info.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((info.getMetaData() >> 1) & 0x1) == 1;

		boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);

		if (isOpen)
		{
			switch (facingPassed + (hingeSide ? 4 : 0))
			{
			case 0:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			case 1:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			case 2:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 3:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 4:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 5:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 6:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			case 7:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			}
		}
		else
		{
			switch (facingPassed)
			{
			case 0:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			case 1:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 2:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 3:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			}
		}
		
		boxes[0].translate(-boxes[0].xw / 2, 0, -boxes[0].zw / 2);

		return boxes;
	}

	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException
	{
		//Ignore all that crap on a slave world
		if (!(world instanceof WorldMaster))
			return voxelData;

		//We should only place the lower part, prevent entities from doing so !
		if (top && entity != null)
			throw new IllegalBlockModificationException("Entities can't place upper doors parts");

		//If the system adds the upper part, no modifications to be done on it
		if (top)
			return voxelData;

		//Check top is free
		int topData = world.getVoxelData(x, y + 1, z);
		if (VoxelFormat.id(topData) != 0)
			throw new IllegalBlockModificationException("Top part isn't free");

		//grab our attributes
		boolean isOpen = ((VoxelFormat.meta(voxelData) >> 0) & 0x1) == 1;
		boolean hingeSide = ((VoxelFormat.meta(voxelData) >> 1) & 0x1) == 1;
		int facingPassed = (VoxelFormat.meta(voxelData) >> 2) & 0x3;

		//Default face is given by passed metadata
		VoxelSides doorSideFacing = VoxelSides.values()[facingPassed];

		//Determine side if placed by an entity and not internal code
		if (entity != null)
		{
			Location loc = entity.getLocation();
			double dx = loc.getX() - (x + 0.5);
			double dz = loc.getZ() - (z + 0.5);
			if (Math.abs(dx) > Math.abs(dz))
			{
				if (dx > 0)
					doorSideFacing = VoxelSides.RIGHT;
				else
					doorSideFacing = VoxelSides.LEFT;
			}
			else
			{
				if (dz > 0)
					doorSideFacing = VoxelSides.FRONT;
				else
					doorSideFacing = VoxelSides.BACK;
			}

			//If there is an adjacent one, set the hinge to right
			int adjacentId = 0;
			switch (doorSideFacing)
			{
			case LEFT:
				adjacentId = world.getVoxelData(x, y, z - 1);
				break;
			case RIGHT:
				adjacentId = world.getVoxelData(x, y, z + 1);
				break;
			case FRONT:
				adjacentId = world.getVoxelData(x - 1, y, z);
				break;
			case BACK:
				adjacentId = world.getVoxelData(x + 1, y, z);
				break;
			default:
				break;
			}
			if (VoxelsStore.get().getVoxelById(adjacentId) instanceof VoxelDoor)
			{
				hingeSide = true;
			}

			voxelData = VoxelFormat.changeMeta(voxelData, computeMeta(isOpen, hingeSide, doorSideFacing));
		}

		//Place the upper part and we're good to go
		world.setVoxelData(x, y + 1, z, VoxelFormat.changeId(voxelData, this.getId() + 1));

		//Return updated voxelData
		return voxelData;
	}

	public static int computeMeta(boolean isOpen, boolean hingeSide, VoxelSides doorFacingSide)
	{
		return computeMeta(isOpen, hingeSide, doorFacingSide.ordinal());
	}

	public static int computeMeta(boolean isOpen, boolean hingeSide, int doorFacingsSide)
	{
		//System.out.println(doorFacingsSide + " open: " + isOpen + " hinge:" + hingeSide);
		return (doorFacingsSide << 2) | (((hingeSide ? 1 : 0) & 0x1) << 1) | (isOpen ? 1 : 0) & 0x1;
	}

	@Override
	public void onRemove(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		//Ignore all that crap on a slave world
		if (!(world instanceof WorldMaster))
			return;

		int otherY = y;
		if (top)
			otherY--;
		else
			otherY++;

		world.setVoxelDataWithoutUpdates(x, y, z, 0);
		int otherData = world.getVoxelData(x, otherY, z);
		//Remove the other part as well, if it still exists
		if (VoxelsStore.get().getVoxelById(otherData) instanceof VoxelDoor)
		{
			world.setVoxelData(x, otherY, z, 0);
		}
	}

	@Override
	public ItemPile[] getItems()
	{
		//Top part shouldn't be placed
		if (top)
			return new ItemPile[] {};


		ItemVoxel itemVoxel = (ItemVoxel)store.parent().items().getItemTypeByName("item_voxel_1x2").newItem();
		itemVoxel.voxel = this;		
		
		return new ItemPile[] { new ItemPile(itemVoxel) };
	}

	@Override
	public int onModification(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException
	{
		return voxelData;
	}
}
