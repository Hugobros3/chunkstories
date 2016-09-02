package io.xol.chunkstories.core.entity;

import java.util.Arrays;

import io.xol.chunkstories.api.entity.interfaces.EntityWithClientPrediction;
import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.item.ItemAk47;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.animation.AnimatedSkeleton;
import io.xol.engine.animation.BVHAnimation;
import io.xol.engine.animation.BVHLibrary;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityHumanoid extends EntityLivingImplentation implements EntityWithClientPrediction
{
	double jumpForce = 0;
	double targetVectorX;
	double targetVectorZ;

	boolean justJumped = false;
	boolean justLanded = false;

	boolean running = false;

	public double maxSpeedRunning = 0.25;
	public double maxSpeed = 0.15;

	public double horizontalSpeed = 0;
	public double metersWalked = 0d;

	public double eyePosition = 1.6;

	public EntityHumanoid(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);

		animatedSkeleton = new EntityHumanoidAnimatedSkeleton();
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
						if (selectedItemPile.getItem() instanceof ItemAk47)
							return BVHLibrary.getAnimation("./animations/human/holding-rifle.bvh");
						else
							return BVHLibrary.getAnimation("./animations/human/holding-item.bvh");
					}
				}
			}

			Vector3d vel = getVelocityComponent().getVelocity();

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
			Vector3d vel = getVelocityComponent().getVelocity();

			double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

			// animationTime += metersWalked * 50;
			//	return BVHLibrary.getAnimation("res/animations/human/running.bvh");

			if (boneName.endsWith("boneHead"))
			{
				Matrix4f modify = getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
				modify.rotate((float) (EntityHumanoid.this.getEntityRotationComponent().getVerticalRotation() / 180 * Math.PI), new Vector3f(0, 1, 0));
				return modify;
			}

			if (horizSpd > 0.030)
				animationTime *= 1.5;
			
			if (horizSpd > 0.060)
				animationTime *= 1.5;
			else if (Arrays.asList(new String[] { "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso" }).contains(boneName))
			{
				//Vector3d vel = getVelocityComponent().getVelocity();
				//double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

				//System.out.println((horizSpd / 0.065) * 0.3);
			}

			return getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
		}

	}

	protected class EntityHumanoidRenderer<H extends EntityHumanoid> implements EntityRenderer<H>
	{

		@Override
		public void setupRender(RenderingContext renderingContext)
		{
			//Player textures
			Texture2D playerTexture = TexturesHandler.getTexture("models/humanoid_test.png");
			playerTexture.setLinearFiltering(false);

			renderingContext.setDiffuseTexture(playerTexture);

			renderingContext.setNormalTexture(TexturesHandler.getTexture("models/humanoid_normal.png"));
			TexturesHandler.getTexture("models/humanoid_normal.png").setLinearFiltering(false);

			renderingContext.setNormalTexture(TexturesHandler.getTexture("textures/normalnormal.png"));
		}

		@Override
		public void forEach(RenderingContext renderingContext, RenderingIterator<H> renderableEntitiesIterator)
		{
			for (EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				ItemPile selectedItemPile = null;

				if (entity instanceof EntityWithSelectedItem)
					selectedItemPile = ((EntityWithSelectedItem) entity).getSelectedItemComponent().getSelectedItem();

				//Prevents laggy behaviour
				Camera cam = renderingContext.getCamera();
				if (entity.equals(Client.getInstance().getClientSideController().getControlledEntity()))
					renderingContext.getCurrentShader().setUniformFloat3("objectPosition", -(float) cam.pos.getX(), -(float) cam.pos.getY() - eyePosition, -(float) cam.pos.getZ());

				//Renders normal limbs
				Matrix4f headRotationMatrix = new Matrix4f();
				headRotationMatrix.translate(new Vector3f(0f, (float) entity.eyePosition, 0f));
				headRotationMatrix.rotate((90 - entity.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
				headRotationMatrix.translate(new Vector3f(0f, -(float) entity.eyePosition, 0f));
				renderingContext.sendTransformationMatrix(headRotationMatrix);

				//Except in fp 
				if (!entity.equals(Client.getInstance().getClientSideController().getControlledEntity()) || renderingContext.isThisAShadowPass())
					ModelLibrary.getRenderableMesh("./models/human.obj").renderButParts(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD");

				//Render rotated limbs
				headRotationMatrix = new Matrix4f();
				headRotationMatrix.translate(new Vector3f(0f, (float) entity.eyePosition, 0f));
				headRotationMatrix.rotate((90 - entity.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));

				if (selectedItemPile != null)
					headRotationMatrix.rotate((-entity.getEntityRotationComponent().getVerticalRotation()) / 180f * 3.14159f, new Vector3f(0, 0, 1));

				headRotationMatrix.translate(new Vector3f(0f, -(float) entity.eyePosition, 0f));
				renderingContext.sendTransformationMatrix(headRotationMatrix);

				if (selectedItemPile != null || !entity.equals(Client.getInstance().getClientSideController().getControlledEntity()) || renderingContext.isThisAShadowPass())
					ModelLibrary.getRenderableMesh("./models/human.obj").renderParts(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD");

				//Matrix to itemInHand bone in the player's bvh
				Matrix4f itemMatrix = new Matrix4f();
				itemMatrix = entity.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix("boneItemInHand", System.currentTimeMillis() % 1000000);
				//System.out.println(itemMatrix);

				Matrix4f.mul(headRotationMatrix, itemMatrix, itemMatrix);

				if (selectedItemPile != null)
					selectedItemPile.getItem().getItemRenderer().renderItemInWorld(renderingContext, selectedItemPile, world, entity.getLocation(), itemMatrix);
			}
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

	public String getDefaultAnimation()
	{
		return "human/standstill";
	}

	@Override
	public void tick()
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
		acceleration = new Vector3d(targetVectorX - getVelocityComponent().getVelocity().getX(), 0, targetVectorZ - getVelocityComponent().getVelocity().getZ());

		//Limit maximal acceleration depending if we're on the groud or not, we accelerate 2x faster on ground
		double maxAcceleration = collision_bot ? 0.010 : 0.005;
		if(inWater)
			maxAcceleration = 0.005;
		if (acceleration.length() > maxAcceleration)
		{
			acceleration.normalize();
			acceleration.scale(maxAcceleration);
		}

		//Tick : will move the entity, solve velocity/acceleration and so on

		
		handleWalkingEtcSounds();
		
		super.tick();
	}

	boolean lastTickOnGround = false;
	
	@Override
	public void tickClientPrediction()
	{
		handleWalkingEtcSounds();
	}

	protected void handleWalkingEtcSounds()
	{
		// Sound stuff
		if (isEntityOnGround() && !lastTickOnGround)
		{
			justLanded = true;
			metersWalked = 0.0;
		}

		//Used to trigger landing sound
		lastTickOnGround = this.isEntityOnGround();
		
		//Bobbing
		Vector3d horizontalSpeed = this.getVelocityComponent().getVelocity().clone();
		horizontalSpeed.setY(0);
		
		if (isEntityOnGround())
			metersWalked += Math.abs(horizontalSpeed.length());
		
		boolean inWater = voxelIn != null && voxelIn.isVoxelLiquid();
		
		Voxel voxelStandingOn = Voxels.get(world.getVoxelData(this.getLocation().clone().add(0.0, -0.01, 0.0)));
		
		if(voxelStandingOn == null || !voxelStandingOn.isVoxelSolid() && !voxelStandingOn.isVoxelLiquid())
			return;
		
		Material material = voxelStandingOn.getMaterial();
		
		if (justJumped && !inWater)
		{
			justJumped = false;
			getWorld().getSoundManager().playSoundEffect(material.resolveProperty("jumpingSounds"), getLocation(), (float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f).setAttenuationEnd(10);
		}
		if (justLanded)
		{
			justLanded = false;
			getWorld().getSoundManager().playSoundEffect(material.resolveProperty("landingSounds"), getLocation(), (float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f).setAttenuationEnd(10);
		}
		
		if (metersWalked > 0.2 * Math.PI * 2)
		{
			metersWalked %= 0.2 * Math.PI * 2;
			if(horizontalSpeed.length() <= 0.06)
				getWorld().getSoundManager().playSoundEffect(material.resolveProperty("walkingSounds"), getLocation(), (float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f).setAttenuationEnd(10);
			else
				getWorld().getSoundManager().playSoundEffect(material.resolveProperty("runningSounds"), getLocation(), (float) (0.9f + Math.sqrt(getVelocityComponent().getVelocity().getX() * getVelocityComponent().getVelocity().getX() + getVelocityComponent().getVelocity().getY() * getVelocityComponent().getVelocity().getY()) * 0.1f), 1f).setAttenuationEnd(10);
			
			//System.out.println("footstep");
			//System.out.println(horizontalSpeed.length());
			//System.out.println(voxelStandingOn.getName() + "	" + material.getName());
		}
		
		//if(this instanceof EntityZombie)
		//	System.out.println(metersWalked + "" + this);
	}

	@Override
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(0.5, 2.00, 0.5) };
	}
}
