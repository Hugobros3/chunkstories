package io.xol.chunkstories.core.entity;

import java.util.Arrays;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithClientPrediction;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldAuthority;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.core.item.ItemFirearm;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.animation.AnimatedSkeleton;
import io.xol.engine.animation.BVHAnimation;
import io.xol.engine.animation.BVHLibrary;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityHumanoid extends EntityLivingImplentation implements EntityWithClientPrediction
{
	double jumpForce = 0;
	protected Vector3dm targetVelocity = new Vector3dm(0);

	boolean justJumped = false;
	boolean justLanded = false;

	boolean running = false;

	public double maxSpeedRunning = 0.25;
	public double maxSpeed = 0.15;

	public double horizontalSpeed = 0;
	public double metersWalked = 0d;

	public double eyePosition = 1.6;

	CachedLodSkeletonAnimator cachedSkeleton;

	public EntityHumanoid(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);

		cachedSkeleton = new CachedLodSkeletonAnimator(new EntityHumanoidAnimatedSkeleton(), 25f, 75f);
		animatedSkeleton = cachedSkeleton;
	}

	protected class EntityHumanoidAnimatedSkeleton extends AnimatedSkeleton
	{
		@Override
		public BVHAnimation getAnimationPlayingForBone(String boneName, double animationTime)
		{
			if (EntityHumanoid.this.isDead())
				return BVHLibrary.getAnimation("./animations/human/ded.bvh");

			if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand" }).contains(boneName))
			{
				if (EntityHumanoid.this instanceof EntityWithSelectedItem)
				{
					ItemPile selectedItemPile = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItemComponent().getSelectedItem();

					//BVHAnimation animation = BVHLibrary.getAnimation("res/animations/human/standstill.bvh");
					if (selectedItemPile != null)
					{
						if (selectedItemPile.getItem() instanceof ItemFirearm)
							return BVHLibrary.getAnimation("./animations/human/holding-rifle.bvh");
						else
							return BVHLibrary.getAnimation("./animations/human/holding-item.bvh");
					}
				}
			}

			Vector3dm vel = getVelocityComponent().getVelocity();

			double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

			if (horizSpd > 0.065)
			{
				//System.out.println("running");
				return BVHLibrary.getAnimation("./animations/human/running.bvh");
			}

			if (horizSpd > 0.0)
				return BVHLibrary.getAnimation("./animations/human/walking.bvh");

			return BVHLibrary.getAnimation("./animations/human/standstill.bvh");
		}

		public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime)
		{
			Vector3dm vel = getVelocityComponent().getVelocity();

			double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

			// animationTime += metersWalked * 50;
			//	return BVHLibrary.getAnimation("res/animations/human/running.bvh");

			if (boneName.endsWith("boneHead"))
			{
				Matrix4f modify = getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
				modify.rotate((float) (EntityHumanoid.this.getEntityRotationComponent().getVerticalRotation() / 180 * Math.PI), new Vector3fm(0, 1, 0));
				return modify;
			}

			if (horizSpd > 0.030)
				animationTime *= 1.5;

			if (horizSpd > 0.060)
				animationTime *= 1.5;
			else if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso" }).contains(boneName))
			{
				//Vector3dm vel = getVelocityComponent().getVelocity();
				//double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

				//System.out.println((horizSpd / 0.065) * 0.3);
			}

			Matrix4f characterRotationMatrix = new Matrix4f();
			//Only the torso is modified, the effect is replicated accross the other bones later
			if (boneName.endsWith("boneTorso"))
				characterRotationMatrix.rotate((90 - getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3fm(0, 0, 1));

			if (Arrays.asList("boneArmLU", "boneArmRU").contains(boneName))
			{
				double k = 0.75;

				ItemPile selectedItem = null;

				if (EntityHumanoid.this instanceof EntityWithSelectedItem)
					selectedItem = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItemComponent().getSelectedItem();

				if (selectedItem != null)
				{
					characterRotationMatrix.translate(new Vector3fm(0f, 0f, (float) k));
					characterRotationMatrix.rotate((getEntityRotationComponent().getVerticalRotation()) / 180f * 3.14159f, new Vector3fm(0, 1, 0));
					characterRotationMatrix.translate(new Vector3fm(0f, 0f, -(float) k));
				}
			}

			return Matrix4f.mul(characterRotationMatrix, getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime), null);
		}

		public boolean shouldHideBone(RenderingInterface renderingContext, String boneName)
		{
			if (EntityHumanoid.this.equals(Client.getInstance().getClientSideController().getControlledEntity()))
			{
				if (renderingContext.isThisAShadowPass())
					return false;

				ItemPile selectedItem = null;

				if (EntityHumanoid.this instanceof EntityWithSelectedItem)
					selectedItem = ((EntityWithSelectedItem) EntityHumanoid.this).getSelectedItemComponent().getSelectedItem();

				if (Arrays.asList("boneArmRU", "boneArmRD").contains(boneName) && selectedItem != null)
					if (selectedItem.getItem() instanceof ItemVoxel)
						return true;

				if (Arrays.asList("boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD").contains(boneName) && selectedItem != null)
					return false;

				return true;
			}
			return false;
		}

	}

	protected class EntityHumanoidRenderer<H extends EntityHumanoid> implements EntityRenderer<H>
	{
		@Override
		public void setupRender(RenderingInterface renderingContext)
		{
			//Player textures
			Texture2D playerTexture = TexturesHandler.getTexture("./models/humanoid_test.png");
			playerTexture.setLinearFiltering(false);

			renderingContext.bindAlbedoTexture(playerTexture);

			TexturesHandler.getTexture("./models/humanoid_normal.png").setLinearFiltering(false);

			//renderingContext.bindNormalTexture(TexturesHandler.getTexture("./models/humanoid_normal.png"));
			renderingContext.bindAlbedoTexture(TexturesHandler.getTexture("./models/humanoid_test.png"));
			renderingContext.bindNormalTexture(TexturesHandler.getTexture("./textures/normalnormal.png"));
			renderingContext.bindMaterialTexture(TexturesHandler.getTexture("./textures/defaultmaterial.png"));
		}

		@Override
		public int forEach(RenderingInterface renderingContext, RenderingIterator<H> renderableEntitiesIterator)
		{
			int e = 0;

			for (EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				Location location = entity.getPredictedLocation();

				if (renderingContext.isThisAShadowPass() && location.distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f)
					continue;

				entity.cachedSkeleton.lodUpdate(renderingContext);

				Matrix4f matrix = new Matrix4f();
				matrix.translate(location.castToSinglePrecision());
				renderingContext.setObjectMatrix(matrix);

				ModelLibrary.getRenderableMesh("./models/human.obj").render(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);
				//animationsData.add(new AnimatableData(location.castToSinglePrecision(), entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, bl, sl));
			}

			//Instanciate all players
			//ModelLibrary.getRenderableMesh("./models/human.obj").renderInstanciated(renderingContext, animationsData);

			//Render items in hands
			for (EntityHumanoid entity : renderableEntitiesIterator)
			{

				if (renderingContext.isThisAShadowPass() && entity.getLocation().distanceTo(renderingContext.getCamera().getCameraPosition()) > 15f)
					continue;

				ItemPile selectedItemPile = null;

				if (entity instanceof EntityWithSelectedItem)
					selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItemComponent().getSelectedItem();

				renderingContext.currentShader().setUniform3f("objectPosition", new Vector3fm(0));

				if (selectedItemPile != null)
				{
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate(entity.getPredictedLocation().castToSinglePrecision());

					Matrix4f.mul(itemMatrix, entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000), itemMatrix);

					selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
				}

				e++;
			}

			return e;
		}

		@Override
		public void freeRessources()
		{

		}

	}

	@Override
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer()
	{
		return new EntityHumanoidRenderer<EntityHumanoid>();
	}

	public Vector3dm getTargetVelocity()
	{
		return targetVelocity;
	}

	@Override
	public void tick(WorldAuthority authority)
	{
		//Only 
		
		boolean tick = false;
		if(this instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) this).getControllerComponent().getController();
			if(controller == null)
				tick = (getWorld() instanceof WorldMaster);
			else if(getWorld() instanceof WorldClient && Client.getInstance().getClientSideController().equals(controller))
				tick = true;
				
		}
		else
			tick = (getWorld() instanceof WorldMaster);
		
		if(tick)
		{
			//The actual moment the jump takes effect
			boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
			if (jumpForce > 0.0 && (!justJumped || inWater))
			{
				//Set the velocity
				getVelocityComponent().setVelocityY(jumpForce);
				justJumped = true;
				metersWalked = 0.0;
				jumpForce = 0.0;
			}

			//Set acceleration vector to wanted speed - actual speed
			acceleration = new Vector3dm(targetVelocity.getX() - getVelocityComponent().getVelocity().getX(), 0, targetVelocity.getZ() - getVelocityComponent().getVelocity().getZ());

			//Limit maximal acceleration depending if we're on the groud or not, we accelerate 2x faster on ground
			double maxAcceleration = collision_bot ? 0.010 : 0.005;
			if (inWater)
				maxAcceleration = 0.005;
			if (acceleration.length() > maxAcceleration)
			{
				acceleration.normalize();
				acceleration.scale(maxAcceleration);
			}
		}
		
		//Plays the walking sounds
		handleWalkingEtcSounds();
		
		//Tick : will move the entity, solve velocity/acceleration and so on
		super.tick(authority);
	}

	boolean lastTickOnGround = false;

	@Override
	public void tickClientPrediction()
	{
		handleWalkingEtcSounds();
	}

	protected void handleWalkingEtcSounds()
	{
		//This is strictly a clientside hack
		if (!(getWorld() instanceof WorldClient))
			return;

		//When the entities are too far from the player, don't play any sounds
		if (Client.getInstance().getClientSideController().getControlledEntity() != null)
			if (Client.getInstance().getClientSideController().getControlledEntity().getLocation().distanceTo(this.getLocation()) > 25f)
				return;

		// Sound stuff
		if (isEntityOnGround() && !lastTickOnGround)
		{
			justLanded = true;
			metersWalked = 0.0;
		}

		//Used to trigger landing sound
		lastTickOnGround = this.isEntityOnGround();

		//Bobbing
		Vector3dm horizontalSpeed = this.getVelocityComponent().getVelocity().clone();
		horizontalSpeed.setY(0d);

		if (isEntityOnGround())
			metersWalked += Math.abs(horizontalSpeed.length());

		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();

		Voxel voxelStandingOn = VoxelsStore.get().getVoxelById(world.getVoxelData(this.getLocation().clone().add(0.0, -0.01, 0.0)));

		if (voxelStandingOn == null || !voxelStandingOn.isVoxelSolid() && !voxelStandingOn.isVoxelLiquid())
			return;

		Material material = voxelStandingOn.getMaterial();

		if (justJumped && !inWater)
		{
			justJumped = false;
			getWorld().getSoundManager()
					.playSoundEffect(material.resolveProperty("jumpingSounds"), getLocation(),
							(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f)
					.setAttenuationEnd(10);
		}
		if (justLanded)
		{
			justLanded = false;
			getWorld().getSoundManager()
					.playSoundEffect(material.resolveProperty("landingSounds"), getLocation(),
							(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f)
					.setAttenuationEnd(10);
		}

		if (metersWalked > 0.2 * Math.PI * 2)
		{
			metersWalked %= 0.2 * Math.PI * 2;
			if (horizontalSpeed.length() <= 0.06)
				getWorld().getSoundManager()
						.playSoundEffect(material.resolveProperty("walkingSounds"), getLocation(),
								(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f),
								1f)
						.setAttenuationEnd(10);
			else
				getWorld().getSoundManager()
						.playSoundEffect(material.resolveProperty("runningSounds"), getLocation(),
								(float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f),
								1f)
						.setAttenuationEnd(10);

		}
	}

	@Override
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(0.5, 1.90, 0.5) };
	}

	@Override
	public Location getPredictedLocation()
	{
		return getLocation();
	}
}
