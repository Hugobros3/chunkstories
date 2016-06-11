package io.xol.chunkstories.entity.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplentation extends EntityImplementation implements EntityLiving
{
	public float health;
	public long lastDamageTook = 0;
	public long damageCooldown = 0;
	
	public EntityLivingImplentation(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		health = getStartHealth();
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
		this.health = health;
	}

	@Override
	public float damage(DamageCause cause, float damage)
	{
		health -= damage;
		return damage;
	}

	@Override
	public boolean isDead()
	{
		return health <= 0;
	}
	
	public void loadCSF(DataInputStream stream) throws IOException
	{
		super.loadCSF(stream);
		health = stream.readFloat();
	}

	/**
	 * Writes the object state to a stream
	 */
	public void saveCSF(DataOutputStream stream) throws IOException
	{
		super.saveCSF(stream);
		stream.writeFloat(health);
	}
}
