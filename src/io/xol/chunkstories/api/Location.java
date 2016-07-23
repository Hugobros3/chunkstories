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
		this(world, position.x, position.y, position.z);
	}

	public Location(Location location)
	{
		this.world = location.getWorld();
		this.x = location.getX();
		this.y = location.getY();
		this.z = location.getZ();
	}

	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public double getZ()
	{
		return z;
	}
	
	public void setX(double x)
	{
		this.x = x;
	}
	
	public void setY(double y)
	{
		this.y = y;
	}
	
	public void setZ(double z)
	{
		this.z = z;
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
		return world.getVoxelData(this, false);
	}

	public void setVoxelDataAtLocation(int voxelData)
	{
		world.setVoxelData(this, voxelData, false);
	}
}
