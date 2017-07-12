package io.xol.chunkstories.core.entity;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.entity.components.EntityComponentRotation;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.events.entity.EntityDamageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer.RenderingPass;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplementation extends EntityBase implements EntityLiving
{
	//Head/body rotation
	EntityComponentRotation entityRotationComponent;

	//Movement stuff
	public Vector3d acceleration = new Vector3d();

	//Damage/health stuff
	private EntityComponentHealth entityHealthComponent;
	private long damageCooldown = 0;
	private DamageCause lastDamageCause;
	long deathDespawnTimer = 6000;

	protected SkeletonAnimator animatedSkeleton;

	protected double lastStandingHeight = Double.NaN;
	protected boolean wasStandingLastTick = true;

	public EntityLivingImplementation(EntityType t, World world, double x, double y, double z)
	{
		super(t, world, x, y, z);

		entityRotationComponent = new EntityComponentRotation(this, this.getComponents().getLastComponent());
		entityHealthComponent = new EntityComponentHealth(this, getStartHealth());
	}
	
	@Override
	public void setLocation(Location loc)
	{
		super.setLocation(loc);
		lastStandingHeight = Double.NaN;
	}
	
	public class HitBoxImpl implements HitBox
	{

		CollisionBox box;
		String skeletonPart;

		public HitBoxImpl(CollisionBox box, String skeletonPart)
		{
			this.box = box;
			this.skeletonPart = skeletonPart;
		}

		public void draw(RenderingInterface context)
		{
			if (!context.currentShader().getShaderName().equals("overlay"))
			{
				context.useShader("overlay");
				context.getCamera().setupShader(context.currentShader());
			}

			context.currentShader().setUniform1i("doTransform", 1);

			Matrix4f boneTransormation = new Matrix4f(EntityLivingImplementation.this.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(skeletonPart, System.currentTimeMillis() % 1000000));

			if (boneTransormation == null)
				return;

			Matrix4f worldPositionTransformation = new Matrix4f();
			
			Location loc = EntityLivingImplementation.this.getLocation();
			Vector3f pos = new Vector3f((float)loc.x, (float)loc.y, (float)loc.z);
			worldPositionTransformation.translate(pos);

			boneTransormation.mul(worldPositionTransformation);

			//Scales/moves the identity box to reflect collisionBox shape
			boneTransormation.translate(new Vector3f((float)box.xpos, (float)box.ypos, (float)box.zpos));
			boneTransormation.scale(new Vector3f((float)box.xw, (float)box.h, (float)box.zw));

			context.currentShader().setUniformMatrix4f("transform", boneTransormation);
			context.unbindAttributes();
			context.bindAttribute("vertexIn", context.meshes().getIdentityCube().asAttributeSource(VertexFormat.FLOAT, 3));

			context.currentShader().setUniform4f("colorIn", 0.0, 1.0, 0.0, 1.0);
			//Check for intersection with player
			EntityControllable ec = ((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity();

			if (ec != null)
			{
				if (lineIntersection((Vector3d) context.getCamera().getCameraPosition(), ((EntityPlayer) ec).getDirectionLookingAt()) != null)
					context.currentShader().setUniform4f("colorIn", 1.0, 0.0, 0.0, 1.0);
			}

			context.draw(Primitive.LINE, 0, 24);
			context.currentShader().setUniform1i("doTransform", 0);
		}

		public Vector3dc lineIntersection(Vector3dc lineStart, Vector3dc lineDirection)
		{
			Matrix4f fromAABBToWorld = new Matrix4f(EntityLivingImplementation.this.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(skeletonPart, System.currentTimeMillis() % 1000000));

			//Fuck off if this has issues
			if (fromAABBToWorld == null)
				return null;

			Matrix4f worldPositionTransformation = new Matrix4f();
			
			Location entityLoc = EntityLivingImplementation.this.getLocation();
			
			Vector3f pos = new Vector3f((float)entityLoc.x, (float)entityLoc.y, (float)entityLoc.z);
			worldPositionTransformation.translate(pos);

			//Creates from AABB space to worldspace
			fromAABBToWorld.mul(worldPositionTransformation);

			//Invert it.
			Matrix4f fromWorldToAABB = new Matrix4f();
			
			fromAABBToWorld.invert();
			//Matrix4f.invert(fromAABBToWorld, fromWorldToAABB);

			//Transform line start into AABB space
			Vector4f lineStart4 = new Vector4f((float)lineStart.x(), (float)lineStart.y(), (float)lineStart.z(), 1.0f);
			Vector4f lineDirection4 = new Vector4f((float)lineDirection.x(), (float)lineDirection.y(), (float)lineDirection.z(), 0.0f);

			//System.out.println(skeletonPart);

			//System.out.println(lineStart4+":"+lineDirection4);

			fromWorldToAABB.transform(lineStart4);
			//Matrix4f.transform(fromWorldToAABB, lineStart4, lineStart4);
			fromWorldToAABB.transform(lineDirection4);
			//Matrix4f.transform(fromWorldToAABB, lineDirection4, lineDirection4);

			//System.out.println(lineStart4+":"+lineDirection4);

			Vector3d lineStartTransformed = new Vector3d(lineStart4.x(), lineStart4.y(), lineStart4.z());
			Vector3d lineDirectionTransformed = new Vector3d(lineDirection4.x(), lineDirection4.y(), lineDirection4.z());

			Vector3dc hitPoint = box.lineIntersection(lineStartTransformed, lineDirectionTransformed);

			//System.out.println(hitPoint);

			if (hitPoint == null)
				return null;

			//Transform hitPoint back into world
			Vector4f hitPoint4 = new Vector4f((float)hitPoint.x(), (float)hitPoint.y(), (float)hitPoint.z(), 1.0f);
			
			fromAABBToWorld.transform(hitPoint4);
			//Matrix4f.transform(fromAABBToWorld, hitPoint4, hitPoint4);

			//hitPoint.set((double) (float) hitPoint4.x(), (double) (float) hitPoint4.y(), (double) (float) hitPoint4.z());
			return new Vector3d((double) (float) hitPoint4.x(), (double) (float) hitPoint4.y(), (double) (float) hitPoint4.z());
		}

		@Override
		public String getName()
		{
			return skeletonPart;
		}
	}

	@Override
	public SkeletonAnimator getAnimatedSkeleton()
	{
		return animatedSkeleton;
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
		return damage(cause, null, damage);
	}

	@Override
	public float damage(DamageCause cause, HitBox osef, float damage)
	{
		if (damageCooldown > System.currentTimeMillis())
			return 0f;

		EntityDamageEvent event = new EntityDamageEvent(this, cause, damage);
		this.getWorld().getGameLogic().getPluginsManager().fireEvent(event);

		if (!event.isCancelled())
		{
			entityHealthComponent.damage(event.getDamageDealt());
			lastDamageCause = cause;

			damageCooldown = System.currentTimeMillis() + cause.getCooldownInMs();

			float damageDealt = event.getDamageDealt();

			//Applies knockback
			if (cause instanceof Entity)
			{
				Entity attacker = (Entity) cause;
				Vector3d attackKnockback = this.getLocation().sub(attacker.getLocation().add(0d, 0d, 0d));
				attackKnockback.y = (0d);
				attackKnockback.normalize();
				
				float knockback = (float) Math.max(1f, Math.pow(damageDealt, 0.5f));
				
				attackKnockback.mul(knockback / 50d);
				attackKnockback.y = (knockback / 50d);
				/*
				attackKnockback.scale(damageDealt / 500d);
				attackKnockback.scale(1.0 / (1.0 + 5 * this.getVelocityComponent().getVelocity().length()));*/

				//.scale(1/60d).scale(damageDealt / 10f);
				this.getVelocityComponent().addVelocity(attackKnockback);
			}

			return damageDealt;
		}

		return 0f;
	}

	@Override
	public void tick()
	{
		if (getWorld() == null)
			return;
		
		//Despawn counter is strictly a client matter
		if (getWorld() instanceof WorldMaster)
		{
			if (isDead())
			{
				deathDespawnTimer--;
				if (deathDespawnTimer < 0)
				{
					world.removeEntity(this);
					return;
				}
			}
			
			//Fall damage
			if(isOnGround())
			{
				if(!wasStandingLastTick && !Double.isNaN(lastStandingHeight))
				{
					double fallDistance = lastStandingHeight - this.getEntityComponentPosition().getLocation().y();
					if(fallDistance > 0)
					{
						//System.out.println("Fell "+fallDistance+" meters");
						if(fallDistance > 5)
						{
							float fallDamage = (float) (fallDistance * fallDistance / 2);
							System.out.println(this + "Took "+fallDamage+" hp of fall damage");
							this.damage(DAMAGE_CAUSE_FALL, fallDamage);
						}
					}
				}
				lastStandingHeight = this.getEntityComponentPosition().getLocation().y();
			}
			this.wasStandingLastTick = isOnGround();
		}

		boolean shouldDoTick = false;
		if (this instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this).getControllerComponent().getController();
			if (controller == null)
				shouldDoTick = (getWorld() instanceof WorldMaster);
			else if (getWorld() instanceof WorldClient && ((WorldClient)getWorld()).getClient().getPlayer().equals(controller))
				shouldDoTick = true;

		}
		else
			shouldDoTick = (getWorld() instanceof WorldMaster);

		if (shouldDoTick)
		{
			Vector3dc ogVelocity = getVelocityComponent().getVelocity();
			Vector3d velocity = new Vector3d(ogVelocity);
			
			Vector2f headRotationVelocity = this.getEntityRotationComponent().tickInpulse();
			getEntityRotationComponent().addRotation(headRotationVelocity.x(), headRotationVelocity.y());

			//voxelIn = VoxelsStore.get().getVoxelById(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
			boolean inWater = isInWater(); //voxelIn.getType().isLiquid();

			// Gravity
			if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().get()))
			{
				double terminalVelocity = inWater ? -0.05 : -0.5;
				if (velocity.y() > terminalVelocity)
					velocity.y = (velocity.y() - 0.008);
				if (velocity.y() < terminalVelocity)
					velocity.y = (terminalVelocity);

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

						acceleration.add(new Vector3d(velocity).normalize().negate().mul(decelerationThen));
						//acceleration.add(0.0, decelerationThen * (velocity.y() > 0.0 ? 1.0 : -1.0), 0.0);
					}
				}
			}

			// Acceleration
			velocity.x = (velocity.x() + acceleration.x());
			velocity.y = (velocity.y() + acceleration.y());
			velocity.z = (velocity.z() + acceleration.z());

			//TODO ugly
			if (!world.isChunkLoaded((int) (double) positionComponent.getLocation().x() / 32, (int) (double) positionComponent.getLocation().y() / 32, (int) (double) positionComponent.getLocation().z() / 32))
			{
				velocity.set(0d, 0d, 0d);
			}

			//Eventually moves
			Vector3dc remainingToMove = moveWithCollisionRestrain(velocity.x(), velocity.y(), velocity.z());
			Vector2d remaining2d = new Vector2d(remainingToMove.x(), remainingToMove.z());

			//Auto-step logic
			if (remaining2d.length() > 0.001 && isOnGround())
			{
				//Cap max speed we can get through the bump ?
				if (remaining2d.length() > 0.20d)
				{
					System.out.println("Too fast, capping");
					remaining2d.normalize();
					remaining2d.mul(0.20);
				}

				//Get whatever we are colliding with

				//Test if setting yourself on top would be ok

				//Do it if possible

				//TODO remake proper
				Vector3d blockedMomentum = new Vector3d(remaining2d.x(), 0, remaining2d.y());
				for (double d = 0.25; d < 0.5; d += 0.05)
				{
					//I don't want any of this to reflect on the object, because it causes ugly jumps in the animation
					Vector3dc canMoveUp = this.canMoveWithCollisionRestrain(new Vector3d(0.0, d, 0.0));
					//It can go up that bit
					if (canMoveUp.length() == 0.0f)
					{
						//Would it help with being stuck ?
						Vector3d tryFromHigher = new Vector3d(this.getLocation());
						tryFromHigher.add(new Vector3d(0.0, d, 0.0));
						Vector3dc blockedMomentumRemaining = this.canMoveWithCollisionRestrain(tryFromHigher, blockedMomentum);
						//If length of remaining momentum < of what we requested it to do, that means it *did* go a bit further away
						if (blockedMomentumRemaining.length() < blockedMomentum.length())
						{
							//Where would this land ?
							Vector3d afterJump = new Vector3d(tryFromHigher);
							afterJump.add(blockedMomentum);
							afterJump.sub(blockedMomentumRemaining);

							//land distance = whatever is left of our -0.55 delta when it hits the ground
							Vector3dc landDistance = this.canMoveWithCollisionRestrain(afterJump, new Vector3d(0.0, -d, 0.0));
							afterJump.add(new Vector3d(0.0, -d, 0.0));
							afterJump.sub(landDistance);

							this.setLocation(new Location(world, afterJump));

							remaining2d = new Vector2d(blockedMomentumRemaining.x(), blockedMomentumRemaining.z());
							break;
						}
					}
				}
			}

			//Collisions, snap to axises
			if (Math.abs(remaining2d.x()) >= 0.001d)
				velocity.x = (0d);
			if (Math.abs(remaining2d.y()) >= 0.001d)
				velocity.z = (0d);
			// Stap it
			if (isOnGround() && velocity.y() < 0)
				velocity.y = (0d);
			else if (isOnGround())
				velocity.y = (0d);

			getVelocityComponent().setVelocity(velocity);
		}
	}

	public boolean isInWater() {
		for(VoxelContext vctx : world.getVoxelsWithin(this.getTranslatedBoundingBox())) {
			if(vctx.getVoxel().getType().isLiquid())
				return true;
		}
		return false;
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

	public Vector3dc getDirectionLookingAt()
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
			double distance = getLocation().distance(renderingContext.getCamera().getCameraPosition());
			double targetFps = renderingContext.renderingConfig().getAnimationCacheFrameRate();//RenderingConfig.animationCacheFrameRate;

			int lodDivisor = 1;
			if (distance > lodStart)
			{
				lodDivisor *= 4;
				if (distance > lodEnd)
					lodDivisor *= 4;
			}
			if (renderingContext.getWorldRenderer().getCurrentRenderingPass() == RenderingPass.SHADOW)
				lodDivisor *= 2;

			targetFps /= lodDivisor;

			double maxMsDiff = 1000.0 / targetFps;
			long time = System.currentTimeMillis();

			//System.out.println("Entity "+distance+" m away, "+targetFps+" target fps, "+maxMsDiff+" ms max diff");

			for (CachedData cachedData : cachedBones.values())
			{
				if (time - cachedData.lastUpdate > maxMsDiff)
					cachedData.needsUpdate = true;
			}
		}

		class CachedData
		{

			Matrix4fc matrix = null;
			long lastUpdate = -1;

			boolean needsUpdate = false;

			CachedData(Matrix4fc matrix, long lastUpdate)
			{
				super();
				this.matrix = matrix;
				this.lastUpdate = lastUpdate;
			}
		}

		@Override
		public Matrix4fc getBoneHierarchyTransformationMatrix(String nameOfEndBone, double animationTime)
		{
			return dataSource.getBoneHierarchyTransformationMatrix(nameOfEndBone, animationTime);
		}

		@Override
		public Matrix4fc getBoneHierarchyTransformationMatrixWithOffset(String nameOfEndBone, double animationTime)
		{
			//if(true)
			//	dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);

			//Don't mess with the client played entity animation, it should NEVER be cached
			if (getWorld() instanceof WorldClient && ((WorldClient)getWorld()).getClient() != null && ((WorldClient)getWorld()).getClient().getPlayer().getControlledEntity() == EntityLivingImplementation.this)
				return dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);

			CachedData cachedData = cachedBones.get(nameOfEndBone);
			//If the matrix exists and doesn't need an update
			if (cachedData != null && !cachedData.needsUpdate)
			{
				cachedData.needsUpdate = false;
				return cachedData.matrix;
			}

			//Obtains the matrix and caches it
			Matrix4fc matrix = dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);
			cachedBones.put(nameOfEndBone, new CachedData(matrix, System.currentTimeMillis()));

			return matrix;
		}

		public boolean shouldHideBone(RenderingInterface renderingContext, String boneName)
		{
			return dataSource.shouldHideBone(renderingContext, boneName);
		}
	}

}
