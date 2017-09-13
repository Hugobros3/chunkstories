package io.xol.chunkstories.core.entity.ai;

import java.util.Random;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.ai.AI;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.sound.SoundSource.Mode;

import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.core.entity.EntityHumanoid;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GenericHumanoidAI extends AI<EntityHumanoid>
{
	static Random rng = new Random();
	
	long counter = 0;
	
	public GenericHumanoidAI(EntityHumanoid entity)
	{
		super(entity);
		currentTask = new AiTaskLookArround(5f);
	}
	
	public void tick()
	{	
		if(entity.isDead())
		{
			//Dead entities shouldn't be moving
			entity.getTargetVelocity().x = (0d);
			entity.getTargetVelocity().z = (0d);
			return;
		}
		
		counter++;
		
		if(currentTask != null)
			currentTask.execute();
		
		//System.out.println(currentTask);
		
		//Random grunge
		if(rng.nextFloat() > 0.9990)
		{
			entity.getWorld().getSoundManager().playSoundEffect("sounds/sfx/zombie.ogg", Mode.NORMAL, entity.getLocation(), (float) (0.9 + Math.random() * 0.2), 1.0f);//.setPitch();
		}
		
		//Water-jump
		if(entity.getWorld().peek(entity.getLocation()).getVoxel().getType().isLiquid())
		{
			if(entity.getVelocityComponent().getVelocity().y() < 0.15)
				entity.getVelocityComponent().addVelocity(0.0, 0.15, 0.0);
			//System.out.println("vel:");
		}
			
	}
	
	class AiTaskLookArround extends AiTask {

		AiTaskLookArround(double lookAtNearbyEntities)
		{
			this.lookAtNearbyEntities = lookAtNearbyEntities;
		}
		
		double targetH = 0;
		double targetV = 0;
		
		double lookAtNearbyEntities;
		int lookAtEntityCoolDown = 60 * 5;
		
		@Override
		public void execute()
		{
			//if(entity.getEntityRotationComponent().getHorizontalRotation() == Float.NaN)
			//	entity.getEntityRotationComponent().setRotation(0.0, 0.0);
			
			if(Math.random() > 0.990)
			{
				targetH = entity.getEntityRotationComponent().getHorizontalRotation() + (Math.random() * 2.0 - 1.0) * 30f;
				
				if(Math.random() > 0.5)
					targetV = targetV / 2.0f + (Math.random() * 2.0 - 1.0) * 20f;
				
				if(targetV > 90f)
					targetV = 90f;
				if(targetV < -90f)
					targetV = -90f;
			}
			
			double diffH = targetH - entity.getEntityRotationComponent().getHorizontalRotation();
			double diffV = targetV - entity.getEntityRotationComponent().getVerticalRotation();
			
			entity.getEntityRotationComponent().addRotation(diffH / 15f, diffV / 15f);
			
			if(lookAtEntityCoolDown > 0)
				lookAtEntityCoolDown--;
			
			if(lookAtNearbyEntities > 0.0 && lookAtEntityCoolDown == 0)
			{
				for(Entity entityToLook : entity.getWorld().getEntitiesInBox(entity.getLocation(), new Vector3d(lookAtNearbyEntities)))
				{
					if(!entityToLook.equals(entity) && entityToLook.getLocation().distance(GenericHumanoidAI.this.entity.getLocation()) <= lookAtNearbyEntities && entityToLook instanceof EntityHumanoid && !((EntityHumanoid) entityToLook).isDead())
					{
						GenericHumanoidAI.this.setAiTask(new AiTaskLookAtEntity((EntityHumanoid) entityToLook, 10f, this));
						lookAtEntityCoolDown = (int) (Math.random() * 60 * 5);
						return;
					}
				}
				
				lookAtEntityCoolDown = (int) (Math.random() * 60);
			}
			
			if(Math.random() > 0.9990)
			{
				GenericHumanoidAI.this.setAiTask(new AiTaskGoSomewhere(
				new Location(entity.getWorld(), entity.getLocation().add((Math.random() * 2.0 - 1.0) * 10, 0d, (Math.random() * 2.0 - 1.0) * 10)), 505));
				return;
			}

			entity.getTargetVelocity().x = (0d);
			entity.getTargetVelocity().z = (0d);
			//entity.getVelocityComponent().setVelocityX(0);
			//entity.getVelocityComponent().setVelocityZ(0);
		}
	}
	
	class AiTaskLookAtEntity extends AiTask {

		EntityHumanoid entityFollowed;
		float maxDistance;
		AiTask previousTask;
		
		int timeBeforeDoingSomethingElse;
		
		public AiTaskLookAtEntity(EntityHumanoid entity, float maxDistance, AiTask previousTask)
		{
			this.entityFollowed = entity;
			this.maxDistance = maxDistance;
			this.previousTask = previousTask;
			this.timeBeforeDoingSomethingElse = (int) (60 * Math.random() * 30);
		}

		@Override
		public void execute()
		{
			timeBeforeDoingSomethingElse--;
			
			if(timeBeforeDoingSomethingElse <= 0 || entityFollowed == null || entityFollowed.isDead())
			{
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			if(entityFollowed.getLocation().distance(entity.getLocation()) > maxDistance)
			{
				//System.out.println("too far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			Vector3d delta = entity.getLocation().sub(entityFollowed.getLocation());
			
			makeEntityLookAt(entity, delta);
			
			entity.getTargetVelocity().x = (0d);
			entity.getTargetVelocity().z = (0d);
			//entity.getVelocityComponent().setVelocityX(0);
			//entity.getVelocityComponent().setVelocityZ(0);
		}
		
	}
	
	class AiTaskGoAtEntity extends AiTask {

		EntityLiving entityFollowed;
		float maxDistance;
		AiTask previousTask;
		
		double entitySpeed = 0.02;
		
		public AiTaskGoAtEntity(EntityLiving entity, float maxDistance, AiTask previousTask)
		{
			this.entityFollowed = entity;
			this.maxDistance = maxDistance;
			this.previousTask = previousTask;
		}

		@Override
		public void execute()
		{
			if(entityFollowed == null || entityFollowed.isDead())
			{
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}

			if(entityFollowed.getLocation().distance(entity.getLocation()) > maxDistance)
			{
				//System.out.println("Entity too far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
				GenericHumanoidAI.this.setAiTask(previousTask);
				return;
			}
			
			Vector3d delta = entityFollowed.getLocation().sub(entity.getLocation());
			
			makeEntityLookAt(entity, new Vector3d(delta).negate());
			
			delta.y = (0d);
			
			//System.out.println("CUCK +"+delta);
			
			delta.normalize().mul(entitySpeed);

			entity.getTargetVelocity().x = (delta.x());
			entity.getTargetVelocity().z = (delta.z());
			
			//entity.getVelocityComponent().setVelocityX(delta.getX());
			//entity.getVelocityComponent().setVelocityZ(delta.getZ());
			
			if(((EntityHumanoid)entity).isOnGround())
			{
				Vector3dc rem = entity.canMoveWithCollisionRestrain(entity.getTargetVelocity());
				
				//rem.setY(0.0D);

				if(Math.sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001)
				//if(rem.length() > 0.001)
					entity.getVelocityComponent().addVelocity(0.0, 0.15, 0.0);
			}
		}
		
	}
	
	protected class AiTaskGoSomewhere extends AiTask {

		Location location;
		int timeOut = -1;
		
		protected AiTaskGoSomewhere(Location location)
		{
			this.location = location;
		}
		
		protected AiTaskGoSomewhere(Location location, int timeOutInTicks)
		{
			this.location = location;
			this.timeOut = timeOutInTicks;
		}
		
		@Override
		public void execute()
		{
			if(timeOut > 0)
				timeOut--;
			
			if(timeOut == 0)
			{
				GenericHumanoidAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}
			
			Vector3d delta = new Vector3d(location).sub(entity.getLocation());
			
			if(delta.length() < 0.25)
			{
				GenericHumanoidAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}	
			
			makeEntityLookAt(entity, new Vector3d(delta).negate());
			
			delta.y = (0d);
			
			double entitySpeed = 0.02;
			
			//System.out.println("CUCK +"+delta);
			
			delta.normalize().mul(entitySpeed);

			entity.getTargetVelocity().x = (delta.x());
			entity.getTargetVelocity().z = (delta.z());
			
			//entity.getVelocityComponent().setVelocityX(delta.getX());
			//entity.getVelocityComponent().setVelocityZ(delta.getZ());
			
			if(((EntityHumanoid)entity).isOnGround())
			{
				Vector3dc rem = entity.canMoveWithCollisionRestrain(entity.getTargetVelocity());
				//rem.setY(0.0D);

				if(Math.sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001)
				//if(rem.length() > 0.001)
					entity.getVelocityComponent().addVelocity(0.0, 0.15, 0.0);
			}
		}
		
	}

	private void makeEntityLookAt(EntityHumanoid entity, Vector3d delta)
	{
		Vector2f deltaHorizontal = new Vector2f((float)(double)delta.x(), (float)(double)delta.z());
		Vector2f deltaVertical = new Vector2f(deltaHorizontal.length(),(float)(double) delta.y());
		deltaHorizontal.normalize();
		deltaVertical.normalize();
		
		double targetH = Math.acos(deltaHorizontal.y()) * 180.0 / Math.PI;
		double targetV = Math.asin(deltaVertical.y()) * 180.0 / Math.PI;
		
		if(deltaHorizontal.x() > 0.0)
			targetH *= -1;
		
		if(targetV > 90f)
			targetV = 90f;
		if(targetV < -90f)
			targetV = -90f;
		
		while(targetH < 0.0)
			targetH += 360.0;
		
		double diffH = targetH - entity.getEntityRotationComponent().getHorizontalRotation();
		
		//Ensures we always take the fastest route
		if(Math.abs(diffH + 360) < Math.abs(diffH))
			diffH = diffH + 360;
		else if(Math.abs(diffH - 360) < Math.abs(diffH))
			diffH = diffH - 360;
		
		double diffV = targetV - entity.getEntityRotationComponent().getVerticalRotation();
		
		if(Double.isNaN(diffH))
			diffH = 0;
		
		if(Double.isNaN(diffV))
			diffV = 0;
		
		entity.getEntityRotationComponent().addRotation(diffH / 15f, diffV / 15f);
	}
}
