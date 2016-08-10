package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.core.entity.components.EntityComponentAnimation;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.animation.AnimatedSkeleton;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplentation extends EntityImplementation implements EntityLiving
{
	public long lastDamageTook = 0;
	public long damageCooldown = 0;
	
	long deathDespawnTimer = 600;

	EntityComponentRotation entityRotationComponent = new EntityComponentRotation(this, this.getComponents().getLastComponent());
	EntityComponentAnimation entityAnimationComponent = new EntityComponentAnimation(this);
	EntityComponentHealth entityHealthComponent;
	
	protected AnimatedSkeleton animatedSkeleton;

	public EntityLivingImplentation(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		entityHealthComponent = new EntityComponentHealth(this, getStartHealth());
	}

	@Override
	public AnimatedSkeleton getAnimatedSkeleton()
	{
		return animatedSkeleton;
	}
	
	public EntityComponentAnimation getAnimationComponent()
	{
		return entityAnimationComponent;
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
		if(isDead())
			deathDespawnTimer--;
		if(deathDespawnTimer < 0)
			this.removeFromWorld();
		
		Vector3d velocity = getVelocityComponent().getVelocity();
		
		Vector2f imp = this.getEntityRotationComponent().tickInpulse();
		getEntityRotationComponent().addRotation(imp.x, imp.y);
		
		voxelIn = VoxelTypes.get(VoxelFormat.id(world.getVoxelData(position.getLocation())));
		boolean inWater = voxelIn.isVoxelLiquid();

		//Collisions
		if (collision_left || collision_right)
			velocity.setX(0);
		if (collision_north || collision_south)
			velocity.setZ(0);
		// Stap it
		if (collision_bot && velocity.getY() < 0)
			velocity.setY(0);
		else if (collision_top)
			velocity.setY(0);

		// Gravity
		if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().isFlying()))
		{
			double terminalVelocity = inWater ? -0.02 : -0.5;
			if (velocity.getY() > terminalVelocity)
				velocity.setY(velocity.getY() - 0.008);
			if (velocity.getY() < terminalVelocity)
				velocity.setY(terminalVelocity);
		}

		// Acceleration
		velocity.setX(velocity.getX() + acceleration.getX());
		velocity.setY(velocity.getY() + acceleration.getY());
		velocity.setZ(velocity.getZ() + acceleration.getZ());

		//TODO ugly
		if (!world.isChunkLoaded((int) position.getLocation().getX() / 32, (int) position.getLocation().getY() / 32, (int) position.getLocation().getZ() / 32))
		{
			velocity.zero();
		}

		//Eventually moves
		blockedMomentum = moveWithCollisionRestrain(velocity.getX(), velocity.getY(), velocity.getZ(), true);
		
		getVelocityComponent().setVelocity(velocity);
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
