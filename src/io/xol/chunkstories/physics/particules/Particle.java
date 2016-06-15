package io.xol.chunkstories.physics.particules;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.world.WorldInterface;
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Particle
{

	public WorldInterface world;
	public double posX, posY, posZ;

	public boolean dead = false;

	public void kill()
	{
		dead = true;
	}

	public Particle(WorldInterface world, double posX, double posY, double posZ)
	{
		this.world = world;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
	}

	public abstract Type getType();

	public void update()
	{

	}

	public abstract String getTextureName();

	public abstract Float getSize();

	public boolean emitsLights()
	{
		return false;
	}

	public Light getLightEmited()
	{
		return null;
	}

	public enum Type
	{
		VOXEL_FRAGMENT, SMOKE, RAINFALL, RAINSMASH, LIGHT, SLIGHT, MUZZLE, BLOOD;
		;
	}
}
