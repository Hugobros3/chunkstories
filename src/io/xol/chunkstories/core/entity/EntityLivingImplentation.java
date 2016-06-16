package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.World;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplentation extends EntityImplementation implements EntityLiving
{
	public float health;
	public long lastDamageTook = 0;
	public long damageCooldown = 0;
	
	EntityComponentRotation entityRotationComponent = new EntityComponentRotation(this, this.getComponents().getLastComponent());
	
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
	
	public EntityComponentRotation getEntityRotationComponent()
	{
		return entityRotationComponent;
	}
	
	public Vector3d getDirectionLookingAt()
	{
		return getEntityRotationComponent().getDirectionLookingAt();
	}
}
