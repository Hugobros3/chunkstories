package io.xol.chunkstories.api;

import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Location extends Vector3d
{
	WorldInterface world;
	
	public Location(WorldInterface world, double x, double y, double z)
	{
		super(x, y, z);
		this.world = world;
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
	
	public WorldInterface getWorld()
	{
		return world;
	}
}
