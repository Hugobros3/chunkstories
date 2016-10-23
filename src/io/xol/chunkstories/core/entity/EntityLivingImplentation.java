package io.xol.chunkstories.core.entity;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentAnimation;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.core.events.EntityDamageEvent;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.animation.SkeletonAnimator;
import io.xol.engine.math.lalgb.Matrix4f;
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

	protected SkeletonAnimator animatedSkeleton;

	DamageCause lastDamageCause;

	public EntityLivingImplentation(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		entityHealthComponent = new EntityComponentHealth(this, getStartHealth());
	}

	@Override
	public SkeletonAnimator getAnimatedSkeleton()
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
		EntityDamageEvent event = new EntityDamageEvent(this, cause, damage);
		this.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

		if (!event.isCancelled())
		{
			entityHealthComponent.damage(event.getDamageDealt());
			lastDamageCause = cause;
			return event.getDamageDealt();
		}

		return 0f;
	}

	@Override
	public void tick()
	{
		if (isDead())
			deathDespawnTimer--;
		if (deathDespawnTimer < 0)
			this.removeFromWorld();

		Vector3d velocity = getVelocityComponent().getVelocity();

		Vector2f headRotationVelocity = this.getEntityRotationComponent().tickInpulse();
		getEntityRotationComponent().addRotation(headRotationVelocity.x, headRotationVelocity.y);

		voxelIn = Voxels.get(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
		boolean inWater = voxelIn.isVoxelLiquid();


		// Gravity
		if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().isFlying()))
		{
			double terminalVelocity = inWater ? -0.05 : -0.5;
			if (velocity.getY() > terminalVelocity)
				velocity.setY(velocity.getY() - 0.008);
			if (velocity.getY() < terminalVelocity)
				velocity.setY(terminalVelocity);

			//Water limits your overall movement
			double targetSpeedInWater = 0.02;
			if (inWater)
			{
				if (velocity.length() > targetSpeedInWater)
				{
					double decelerationThen = Math.pow((velocity.length() - targetSpeedInWater), 1.0);

					//System.out.println(decelerationThen);
					double maxDeceleration = 0.006;
					if (decelerationThen > maxDeceleration)
						decelerationThen = maxDeceleration;

					//System.out.println(decelerationThen);

					acceleration.add(velocity.clone().normalize().negate().scale(decelerationThen));
					//acceleration.add(0.0, decelerationThen * (velocity.getY() > 0.0 ? 1.0 : -1.0), 0.0);
				}
			}
		}

		// Acceleration
		velocity.setX(velocity.getX() + acceleration.getX());
		velocity.setY(velocity.getY() + acceleration.getY());
		velocity.setZ(velocity.getZ() + acceleration.getZ());

		//TODO ugly
		if (!world.isChunkLoaded((int) positionComponent.getLocation().getX() / 32, (int) positionComponent.getLocation().getY() / 32, (int) positionComponent.getLocation().getZ() / 32))
		{
			velocity.zero();
		}
		
		//Eventually moves
		blockedMomentum = moveWithCollisionRestrain(velocity.getX(), velocity.getY(), velocity.getZ(), true);

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

	@Override
	public DamageCause getLastDamageCause()
	{
		return lastDamageCause;
	}

	@Override
	public String getName()
	{
		return this.getClass().getSimpleName();
	}
	
	public class CachedLodSkeletonAnimator implements SkeletonAnimator
	{
		Map<String, CachedData> cachedBones = new HashMap<String, CachedData>();
		SkeletonAnimator dataSource;
		double lodStart;
		double lodEnd;
		
		public CachedLodSkeletonAnimator(SkeletonAnimator dataSource, double lodStart, double lodEnd)
		{
			this.dataSource = dataSource;
			this.lodStart = lodStart;
			this.lodEnd = lodEnd;
		}
		
		public void lodUpdate(RenderingInterface renderingContext)
		{
			
			double distance = getLocation().distanceTo(renderingContext.getCamera().getCameraPosition());
			double targetFps = RenderingConfig.animationCacheFrameRate;
			
			int lodDivisor = 1;
			if(distance > lodStart)
			{
				lodDivisor *= 4;
				if(distance > lodEnd)
					lodDivisor *= 4;
			}
			if(renderingContext.isThisAShadowPass())
				lodDivisor *= 2;
			
			targetFps /= lodDivisor;
			
			double maxMsDiff = 1000.0 / targetFps;
			long time = System.currentTimeMillis();
			
			//System.out.println("Entity "+distance+" m away, "+targetFps+" target fps, "+maxMsDiff+" ms max diff");
			
			for(CachedData cachedData : cachedBones.values())
			{
				if(time - cachedData.lastUpdate > maxMsDiff)
					cachedData.needsUpdate = true;
			}
		}
		
		class CachedData {
			
			Matrix4f matrix = null;
			long lastUpdate = -1;
			
			boolean needsUpdate = false;
			
			CachedData(Matrix4f matrix, long lastUpdate)
			{
				super();
				this.matrix = matrix;
				this.lastUpdate = lastUpdate;
			}
		}

		@Override
		public Matrix4f getBoneHierarchyTransformationMatrix(String nameOfEndBone, double animationTime)
		{
			return dataSource.getBoneHierarchyTransformationMatrix(nameOfEndBone, animationTime);
		}

		@Override
		public Matrix4f getBoneHierarchyTransformationMatrixWithOffset(String nameOfEndBone, double animationTime)
		{
			//if(true)
			//	dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);
			
			//Don't mess with the client
			if(Client.getInstance() != null && Client.getInstance().getClientSideController().getControlledEntity() == EntityLivingImplentation.this)
				return dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);
			
			CachedData cachedData = cachedBones.get(nameOfEndBone);
			//If the matrix exists and doesn't need an update
			if(cachedData != null && !cachedData.needsUpdate)
			{
				cachedData.needsUpdate = false;
				return cachedData.matrix.clone();
			}
			
			//Obtains the matrix and caches it
			Matrix4f matrix = dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);
			cachedBones.put(nameOfEndBone, new CachedData(matrix, System.currentTimeMillis()));
			
			return matrix.clone();
		}
		
		public boolean shouldHideBone(RenderingInterface renderingContext, String boneName)
		{
			return dataSource.shouldHideBone(renderingContext, boneName);
		}
	}

}
