package io.xol.chunkstories.api;

import io.xol.chunkstories.api.math.vector.Vector3;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Location extends Vector3dm
{
	World world;
	
	public Location(World world, double x, double y, double z)
	{
		super(x, y, z);
		this.world = world;
	}
	
	public Location(World world, Vector3<Double> position)
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
