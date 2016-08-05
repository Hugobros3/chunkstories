package io.xol.chunkstories.core.entity;

import java.util.Arrays;

import io.xol.chunkstories.api.entity.interfaces.EntityWithSelectedItem;
import io.xol.chunkstories.core.item.ItemAk47;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.animation.AnimatedSkeleton;
import io.xol.engine.animation.BVHAnimation;
import io.xol.engine.animation.BVHLibrary;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityHumanoid extends EntityLivingImplentation
{

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
