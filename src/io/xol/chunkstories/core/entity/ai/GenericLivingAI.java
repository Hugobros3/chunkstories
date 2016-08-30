package io.xol.chunkstories.core.entity.ai;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.ai.AI;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.core.entity.EntityLivingImplentation;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GenericLivingAI extends AI<EntityLiving>
{
	public GenericLivingAI(EntityLiving entity)
	{
		super(entity);
		currentTask = new AiTaskLookArround(5f);
	}
	
	public void tick()
	{
		if(entity.isDead())
		{
			entity.getVelocityComponent().setVelocityX(0);
			entity.getVelocityComponent().setVelocityZ(0);
			return;
		}
		
		if(currentTask != null)
			currentTask.execute();
		
		if(Math.random() > 0.9990)
		{
			entity.getWorld().getSoundManager().playSoundEffect("sounds/sfx/zombie.ogg", entity.getLocation(), (float) (0.9 + Math.random() * 0.2), 1.0f);//.setPitch();
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
				for(Entity entityToLook : entity.getWorld().getAllLoadedEntities())
				{
					if(!entityToLook.equals(entity) && entityToLook.getLocation().distanceTo(GenericLivingAI.this.entity.getLocation()) <= lookAtNearbyEntities && entityToLook instanceof EntityLiving && !((EntityLiving) entityToLook).isDead())
					{
						GenericLivingAI.this.setAiTask(new AiTaskLookAtEntity((EntityLiving) entityToLook, 10f, this));
						lookAtEntityCoolDown = (int) (Math.random() * 60 * 5);
						return;
					}
				}
			}
			
			if(Math.random() > 0.9990)
			{
				GenericLivingAI.this.setAiTask(new AiTaskGoSomewhere(
				new Location(entity.getWorld(), entity.getLocation().clone().add((Math.random() * 2.0 - 1.0) * 10, 0, (Math.random() * 2.0 - 1.0) * 10)), 505));
				return;
			}

			entity.getVelocityComponent().setVelocityX(0);
			entity.getVelocityComponent().setVelocityZ(0);
		}
	}
	
	class AiTaskLookAtEntity extends AiTask {

		EntityLiving entityFollowed;
		float maxDistance;
		AiTask previousTask;
		
		int timeBeforeDoingSomethingElse;
		
		public AiTaskLookAtEntity(EntityLiving entity, float maxDistance, AiTask previousTask)
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
				GenericLivingAI.this.setAiTask(previousTask);
				return;
			}

			if(entityFollowed.getLocation().distanceTo(entity.getLocation()) > maxDistance)
			{
				System.out.println("too far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
				GenericLivingAI.this.setAiTask(previousTask);
				return;
			}

			Vector3d delta = entity.getLocation().clone().sub(entityFollowed.getLocation());
			
			makeEntityLookAt(entity, delta);
			
			entity.getVelocityComponent().setVelocityX(0);
			entity.getVelocityComponent().setVelocityZ(0);
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
				GenericLivingAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}
			
			Vector3d delta = location.clone().sub(entity.getLocation());
			
			if(delta.length() < 0.25)
			{
				GenericLivingAI.this.setAiTask(new AiTaskLookArround(5f));
				return;
			}	
			
			double entitySpeed = 0.02;
			
			//System.out.println("CUCK +"+delta);
			
			delta.normalize().scale(entitySpeed);
			entity.getVelocityComponent().setVelocityX(delta.getX());
			entity.getVelocityComponent().setVelocityZ(delta.getZ());
			
			if(((EntityLivingImplentation)entity).collision_bot)
			{
				if(		((EntityLivingImplentation)entity).collision_left || 
						((EntityLivingImplentation)entity).collision_right || 
						((EntityLivingImplentation)entity).collision_north || 
						((EntityLivingImplentation)entity).collision_south)
				entity.getVelocityComponent().addVelocity(0.0, 0.15, 0.0);
			}
			
			makeEntityLookAt(entity, delta.clone().negate());
		}
		
	}

	private void makeEntityLookAt(EntityLiving entity, Vector3d delta)
	{
		Vector2f deltaHorizontal = new Vector2f((float)delta.getX(), (float)delta.getZ());
		Vector2f deltaVertical = new Vector2f(deltaHorizontal.length(),(float) delta.getY());
		deltaHorizontal.normalise();
		deltaVertical.normalise();
		
		double targetH = Math.acos(deltaHorizontal.y) * 180.0 / Math.PI;
		double targetV = Math.asin(deltaVertical.y) * 180.0 / Math.PI;
		
		if(deltaHorizontal.x > 0.0)
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
