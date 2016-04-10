package io.xol.chunkstories.entity.core;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.entity.EntityImpl;
import io.xol.chunkstories.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImpl extends EntityImpl implements EntityLiving
{
	public float life;
	
	public EntityLivingImpl(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		life = getStartHealth();
	}

	@Override
	public float getMaxHealth()
	{
		return 100;
	}

	@Override
	public float getStartHealth()
	{
		return 100;
	}

	@Override
	public void setHealth(float health)
	{
		life = health;
	}

	@Override
	public float damage(DamageCause cause, float damage)
	{
		life -= damage;
		return damage;
	}

	@Override
	public boolean isDead()
	{
		return life <= 0;
	}
}
