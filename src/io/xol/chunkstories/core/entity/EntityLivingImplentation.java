package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplentation extends EntityImplementation implements EntityLiving
{
	public long lastDamageTook = 0;
	public long damageCooldown = 0;

	EntityComponentRotation entityRotationComponent = new EntityComponentRotation(this, this.getComponents().getLastComponent());
	EntityComponentHealth entityHealthComponent;// = new EntityComponentHealth(this);

	public EntityLivingImplentation(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		entityHealthComponent = new EntityComponentHealth(this, getStartHealth());
	}

	@Override
	public float getMaxHealth()
	{
		return 100;
	}

	@Override
	public float getStartHealth()
	{
		return getMaxHealth();
	}

	@Override
	public void setHealth(float health)
	{
		entityHealthComponent.setHealth(health);
		//this.health = health;
	}

	public float getHealth()
	{
		return entityHealthComponent.getHealth();
	}

	@Override
	public float damage(DamageCause cause, float damage)
	{
		entityHealthComponent.damage(damage);
		return damage;
	}

	@Override
	public void tick()
	{
		//Irrelevant.
		//pos.x %= world.getWorldSize();
		//pos.z %= world.getWorldSize();

		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getVoxelData(position.getLocation())));
		boolean inWater = voxelIn.isVoxelLiquid();

		// vel.z=Math.cos(a)*hSpeed*0.1;
		if (collision_left || collision_right)
			vel.x = 0;
		if (collision_north || collision_south)
			vel.z = 0;
		// Stap it
		if (collision_bot && vel.y < 0)
			vel.y = 0;
		else if (collision_top)
			vel.y = 0;

		// Gravity
		if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().isFlying()))
		{
			double terminalVelocity = inWater ? -0.02 : -0.5;
			if (vel.y > terminalVelocity)
				vel.y -= 0.008;
			if (vel.y < terminalVelocity)
				vel.y = terminalVelocity;
		}

		// Acceleration
		vel.x += acc.x;
		vel.y += acc.y;
		vel.z += acc.z;

		//TODO ugly
		if (!world.isChunkLoaded((int) position.getLocation().x / 32, (int) position.getLocation().y / 32, (int) position.getLocation().z / 32))
		{
			vel.zero();
		}

		//TODO use vector3d there
		blockedMomentum = moveWithCollisionRestrain(vel.x, vel.y, vel.z, true);
	}

	@Override
	public boolean isDead()
	{
		return getHealth() <= 0;
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
