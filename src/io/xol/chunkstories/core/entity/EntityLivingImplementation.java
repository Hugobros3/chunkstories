package io.xol.chunkstories.core.entity;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.core.entity.components.EntityComponentHealth;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.chunkstories.core.events.EntityDamageEvent;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.engine.animation.SkeletonAnimator;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.dp.Vector2dm;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.sp.Vector2fm;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityLivingImplementation extends EntityImplementation implements EntityLiving
{
	//Head/body rotation
	EntityComponentRotation entityRotationComponent;

	//Movement stuff
	public Vector3dm acceleration = new Vector3dm();

	//Damage/health stuff
	private EntityComponentHealth entityHealthComponent;
	private long damageCooldown = 0;
	private DamageCause lastDamageCause;
	long deathDespawnTimer = 6000;

	protected SkeletonAnimator animatedSkeleton;

	protected double lastStandingHeight = Double.NaN;
	protected boolean wasStandingLastTick = true;

	public EntityLivingImplementation(World world, double x, double y, double z)
	{
		super(world, x, y, z);

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

			Matrix4f boneTransormation = EntityLivingImplementation.this.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(skeletonPart, System.currentTimeMillis() % 1000000);

			if (boneTransormation == null)
				return;

			Matrix4f worldPositionTransformation = new Matrix4f();
			Vector3fm pos = EntityLivingImplementation.this.getLocation().castToSinglePrecision();
			worldPositionTransformation.translate(pos);

			boneTransormation.multiply(worldPositionTransformation);

			//Scales/moves the identity box to reflect collisionBox shape
			boneTransormation.translate(new Vector3fm(box.xpos, box.ypos, box.zpos));
			boneTransormation.scale(new Vector3fm(box.xw, box.h, box.zw));

			context.currentShader().setUniformMatrix4f("transform", boneTransormation);
			context.unbindAttributes();
			context.bindAttribute("vertexIn", OverlayRenderer.getCube().asAttributeSource(VertexFormat.FLOAT, 3));

			context.currentShader().setUniform4f("colorIn", 0.0, 1.0, 0.0, 1.0);
			//Check for intersection with player
			EntityControllable ec = Client.getInstance().getPlayer().getControlledEntity();

			if (ec != null)
			{
				if (lineIntersection((Vector3dm) context.getCamera().getCameraPosition(), ((EntityPlayer) ec).getDirectionLookingAt()) != null)
					context.currentShader().setUniform4f("colorIn", 1.0, 0.0, 0.0, 1.0);
			}

			context.draw(Primitive.LINE, 0, 24);
			context.currentShader().setUniform1i("doTransform", 0);
		}

		public Vector3dm lineIntersection(Vector3dm lineStart, Vector3dm lineDirection)
		{
			Matrix4f fromAABBToWorld = EntityLivingImplementation.this.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(skeletonPart, System.currentTimeMillis() % 1000000);

			//Fuck off if this has issues
			if (fromAABBToWorld == null)
				return null;

			Matrix4f worldPositionTransformation = new Matrix4f();
			Vector3fm pos = EntityLivingImplementation.this.getLocation().castToSinglePrecision();
			worldPositionTransformation.translate(pos);

			//Creates from AABB space to worldspace
			fromAABBToWorld.multiply(worldPositionTransformation);

			//Invert it.
			Matrix4f fromWorldToAABB = new Matrix4f();
			Matrix4f.invert(fromAABBToWorld, fromWorldToAABB);

			//Transform line start into AABB space
			Vector4fm lineStart4 = new Vector4fm(lineStart.getX(), lineStart.getY(), lineStart.getZ(), 1.0f);
			Vector4fm lineDirection4 = new Vector4fm(lineDirection.getX(), lineDirection.getY(), lineDirection.getZ(), 0.0f);

			//System.out.println(skeletonPart);

			//System.out.println(lineStart4+":"+lineDirection4);

			Matrix4f.transform(fromWorldToAABB, lineStart4, lineStart4);
			Matrix4f.transform(fromWorldToAABB, lineDirection4, lineDirection4);

			//System.out.println(lineStart4+":"+lineDirection4);

			Vector3dm lineStartTransformed = new Vector3dm(lineStart4.getX(), lineStart4.getY(), lineStart4.getZ());
			Vector3dm lineDirectionTransformed = new Vector3dm(lineDirection4.getX(), lineDirection4.getY(), lineDirection4.getZ());

			Vector3dm hitPoint = box.lineIntersection(lineStartTransformed, lineDirectionTransformed);

			//System.out.println(hitPoint);

			if (hitPoint == null)
				return null;

			//Transform hitPoint back into world
			Vector4fm hitPoint4 = new Vector4fm(hitPoint.getX(), hitPoint.getY(), hitPoint.getZ(), 1.0f);
			Matrix4f.transform(fromAABBToWorld, hitPoint4, hitPoint4);

			hitPoint.set((double) (float) hitPoint4.getX(), (double) (float) hitPoint4.getY(), (double) (float) hitPoint4.getZ());
			return hitPoint;
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
				Vector3dm attackKnockback = this.getLocation().sub(attacker.getLocation().add(0d, 0d, 0d));
				attackKnockback.setY(0d);
				attackKnockback.normalize();
				attackKnockback.setY(0.35);
				attackKnockback.scale(damageDealt / 500d);
				attackKnockback.scale(1.0 / (1.0 + 5 * this.getVelocityComponent().getVelocity().length()));

				//.scale(1/60d).scale(damageDealt / 10f);
				this.getVelocityComponent().addVelocity(attackKnockback);
			}

			return damageDealt;
		}

		return 0f;
	}

	@Override
	public void tick(WorldAuthority authority)
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
					double fallDistance = lastStandingHeight - this.getEntityComponentPosition().getLocation().getY();
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
				lastStandingHeight = this.getEntityComponentPosition().getLocation().getY();
			}
			this.wasStandingLastTick = isOnGround();
		}

		boolean shouldDoTick = false;
		if (this instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this).getControllerComponent().getController();
			if (controller == null)
				shouldDoTick = (getWorld() instanceof WorldMaster);
			else if (getWorld() instanceof WorldClient && Client.getInstance().getPlayer().equals(controller))
				shouldDoTick = true;

		}
		else
			shouldDoTick = (getWorld() instanceof WorldMaster);

		if (shouldDoTick)
		{
			Vector3dm velocity = getVelocityComponent().getVelocity();

			Vector2fm headRotationVelocity = this.getEntityRotationComponent().tickInpulse();
			getEntityRotationComponent().addRotation(headRotationVelocity.getX(), headRotationVelocity.getY());

			voxelIn = VoxelsStore.get().getVoxelById(VoxelFormat.id(world.getVoxelData(positionComponent.getLocation())));
			boolean inWater = voxelIn.isVoxelLiquid();

			// Gravity
			if (!(this instanceof EntityFlying && ((EntityFlying) this).getFlyingComponent().get()))
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
			if (!world.isChunkLoaded((int) (double) positionComponent.getLocation().getX() / 32, (int) (double) positionComponent.getLocation().getY() / 32, (int) (double) positionComponent.getLocation().getZ() / 32))
			{
				velocity.set(0d, 0d, 0d);
			}

			//Eventually moves
			Vector3dm remainingToMove = moveWithCollisionRestrain(velocity.getX(), velocity.getY(), velocity.getZ());
			Vector2dm remaining2d = new Vector2dm(remainingToMove.getX(), remainingToMove.getZ());

			//Auto-step logic
			if (remaining2d.length() > 0.001 && isOnGround())
			{
				//Cap max speed we can get through the bump ?
				if (remaining2d.length() > 0.20d)
				{
					System.out.println("Too fast, capping");
					remaining2d.normalize();
					remaining2d.scale(0.20);
				}

				//Get whatever we are colliding with

				//Test if setting yourself on top would be ok

				//Do it if possible

				//TODO remake proper
				Vector3dm blockedMomentum = new Vector3dm(remaining2d.getX(), 0, remaining2d.getY());
				for (double d = 0.25; d < 0.5; d += 0.05)
				{
					//I don't want any of this to reflect on the object, because it causes ugly jumps in the animation
					Vector3dm canMoveUp = this.canMoveWithCollisionRestrain(new Vector3dm(0.0, d, 0.0));
					//It can go up that bit
					if (canMoveUp.length() == 0.0f)
					{
						//Would it help with being stuck ?
						Vector3dm tryFromHigher = new Vector3dm(this.getLocation());
						tryFromHigher.add(new Vector3dm(0.0, d, 0.0));
						Vector3dm blockedMomentumRemaining = this.canMoveWithCollisionRestrain(tryFromHigher, blockedMomentum);
						//If length of remaining momentum < of what we requested it to do, that means it *did* go a bit further away
						if (blockedMomentumRemaining.length() < blockedMomentum.length())
						{
							//Where would this land ?
							Vector3dm afterJump = new Vector3dm(tryFromHigher);
							afterJump.add(blockedMomentum);
							afterJump.sub(blockedMomentumRemaining);

							//land distance = whatever is left of our -0.55 delta when it hits the ground
							Vector3dm landDistance = this.canMoveWithCollisionRestrain(afterJump, new Vector3dm(0.0, -d, 0.0));
							afterJump.add(new Vector3dm(0.0, -d, 0.0));
							afterJump.sub(landDistance);

							this.setLocation(new Location(world, afterJump));

							remaining2d = new Vector2dm(blockedMomentumRemaining.getX(), blockedMomentumRemaining.getZ());
							break;
						}
					}
				}
			}

			//Collisions, snap to axises
			if (Math.abs(remaining2d.getX()) >= 0.001d)
				velocity.setX(0d);
			if (Math.abs(remaining2d.getY()) >= 0.001d)
				velocity.setZ(0d);
			// Stap it
			if (isOnGround() && velocity.getY() < 0)
				velocity.setY(0d);
			else if (isOnGround())
				velocity.setY(0d);

			getVelocityComponent().setVelocity(velocity);
		}
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

	public Vector3dm getDirectionLookingAt()
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
			if (distance > lodStart)
			{
				lodDivisor *= 4;
				if (distance > lodEnd)
					lodDivisor *= 4;
			}
			if (renderingContext.isThisAShadowPass())
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
			if (Client.getInstance() != null && Client.getInstance().getPlayer().getControlledEntity() == EntityLivingImplementation.this)
				return dataSource.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone, animationTime);

			CachedData cachedData = cachedBones.get(nameOfEndBone);
			//If the matrix exists and doesn't need an update
			if (cachedData != null && !cachedData.needsUpdate)
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
