package io.xol.chunkstories.core.entity;

import java.util.Arrays;

import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.chunkstories.api.rendering.entity.EntityRenderer;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.item.ItemAk47;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.Camera;
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

public abstract class EntityHumanoid extends EntityLivingImplentation
{
	public double eyePosition = 1.6;
	
	public EntityHumanoid(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		
		animatedSkeleton = new EntityHumanoidAnimatedSkeleton();
	}
	
	protected class EntityHumanoidAnimatedSkeleton extends AnimatedSkeleton {

		@Override
		public BVHAnimation getAnimationPlayingForBone(String boneName, double animationTime)
		{
			if(EntityHumanoid.this.isDead())
				return BVHLibrary.getAnimation("res/animations/human/ded.bvh");
			
			if(Arrays.asList(new String[] {"boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand"}).contains(boneName))
			{
				if(EntityHumanoid.this instanceof EntityWithSelectedItem)
				{
					ItemPile selectedItemPile = ((EntityWithSelectedItem)EntityHumanoid.this).getSelectedItemComponent().getSelectedItem();
					
					//BVHAnimation animation = BVHLibrary.getAnimation("res/animations/human/standstill.bvh");
					if (selectedItemPile != null)
					{
						if (selectedItemPile.getItem() instanceof ItemAk47)
							return BVHLibrary.getAnimation("res/animations/human/holding-rifle.bvh");
						else
							return BVHLibrary.getAnimation("res/animations/human/holding-item.bvh");
					}
				}
			}
			
			Vector3d vel = getVelocityComponent().getVelocity();
			
			double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
			
			if(horizSpd > 0.065)
			{
				//System.out.println("running");
				return BVHLibrary.getAnimation("res/animations/human/running.bvh");
			}
			
			if(horizSpd > 0.0)
				return BVHLibrary.getAnimation("res/animations/human/walking.bvh");
			
			return BVHLibrary.getAnimation("res/animations/human/standstill.bvh");
		}
		
		public Matrix4f getBoneTransformationMatrix(String boneName, double animationTime)
		{
			Vector3d vel = getVelocityComponent().getVelocity();
			
			double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
			

			animationTime *= 1.0 + (horizSpd / 0.065) * 0.3;
			//	return BVHLibrary.getAnimation("res/animations/human/running.bvh");
			
			if(boneName.endsWith("boneHead"))
			{
				Matrix4f modify = getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
				modify.rotate((float) (EntityHumanoid.this.getEntityRotationComponent().getVerticalRotation() / 180 * Math.PI), new Vector3f(0, 1, 0));
				return modify;
			}
			

			if(horizSpd > 0.065)
				animationTime *= 2.0;
			else if(Arrays.asList(new String[] {"boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso"}).contains(boneName))
			{
				//Vector3d vel = getVelocityComponent().getVelocity();
				//double horizSpd = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
				
				//System.out.println((horizSpd / 0.065) * 0.3);
			}
			
			return getAnimationPlayingForBone(boneName, animationTime).getBone(boneName).getTransformationMatrix(animationTime);
		}
		
	}
	
	protected class EntityHumanoidRenderer<H extends EntityHumanoid> implements EntityRenderer<H> {

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
			for(EntityHumanoid entity : renderableEntitiesIterator.getElementsInFrustrumOnly())
			{
				ItemPile selectedItemPile = null;
				
				if(entity instanceof EntityWithSelectedItem)
					selectedItemPile = ((EntityWithSelectedItem)entity).getSelectedItemComponent().getSelectedItem();
				
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
					ModelLibrary.getRenderableMesh("res/models/human.obj").renderButParts(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD");
				
				//Render rotated limbs
				headRotationMatrix = new Matrix4f();
				headRotationMatrix.translate(new Vector3f(0f, (float) entity.eyePosition, 0f));
				headRotationMatrix.rotate((90 - entity.getEntityRotationComponent().getHorizontalRotation()) / 180f * 3.14159f, new Vector3f(0, 1, 0));
				
				if(selectedItemPile != null)
					headRotationMatrix.rotate((-entity.getEntityRotationComponent().getVerticalRotation()) / 180f * 3.14159f, new Vector3f(0, 0, 1));
				
				headRotationMatrix.translate(new Vector3f(0f, -(float) entity.eyePosition, 0f));
				renderingContext.sendTransformationMatrix(headRotationMatrix);

				if(selectedItemPile != null || !entity.equals(Client.getInstance().getClientSideController().getControlledEntity()) || renderingContext.isThisAShadowPass())
					ModelLibrary.getRenderableMesh("res/models/human.obj").renderParts(renderingContext, entity.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000, "boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD");
			
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
	public CollisionBox[] getCollisionBoxes()
	{
		return new CollisionBox[] { new CollisionBox(0.5, 2.00, 0.5) };
	}
}
