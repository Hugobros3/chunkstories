package io.xol.chunkstories.api;

import io.xol.chunkstories.api.world.World;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Location extends Vector3d
{
	World world;
	
	public Location(World world, double x, double y, double z)
	{
		super(x, y, z);
		this.world = world;
	}
	
	public Location(World world, Vector3d position)
	{
		this(world, position.getX(), position.getY(), position.getZ());
	}

	public Location(Location location)
	{
		this.world = location.getWorld();
		this.setX(location.getX());
		this.setY(location.getY());
		this.setZ(location.getZ());
	}
	
	public World getWorld()
	{
		return world;
	}

	public void setWorld(World world)
	{
		this.world = world;
	}

	public int getVoxelDataAtLocation()
	{
		return world.getVoxelData(this);
	}

	public void setVoxelDataAtLocation(int voxelData)
	{
		world.setVoxelData(this, voxelData);
	}
}
