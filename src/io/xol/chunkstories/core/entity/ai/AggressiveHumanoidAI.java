package io.xol.chunkstories.core.entity.ai;

import java.util.Collection;

import io.xol.chunkstories.api.ai.AI;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.core.entity.EntityHumanoid;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class AggressiveHumanoidAI extends GenericHumanoidAI
{
	Collection<Class<? extends Entity>> targetsTypes;

	double aggroRadius;
	int attackEntityCooldown = 60 * 5;

	public AggressiveHumanoidAI(EntityHumanoid entity, double aggroRadius, Collection<Class<? extends Entity>> targetsTypes)
	{
		super(entity);
		//this.currentTask = new AiTaskLookArroundAndSearchTarget(aggroRadius);
		this.aggroRadius = aggroRadius;
		this.targetsTypes = targetsTypes;
	}

	public void tick()
	{
		super.tick();

		if(entity.isDead())
			return;
		
		if (attackEntityCooldown > 0)
			attackEntityCooldown--;

		//Find entities to attack
		if (!(this.currentTask instanceof AiTaskAttackEntity) && aggroRadius > 0.0 && attackEntityCooldown == 0)
		{
			//Only look for them once in 2s
			attackEntityCooldown = (int) (Math.random() * 60 * 2);

			for (Entity entityToLook : entity.getWorld().getAllLoadedEntities())
			{
				if (!entityToLook.equals(entity) && entityToLook.getLocation().distanceTo(entity.getLocation()) <= aggroRadius && entityToLook instanceof EntityHumanoid && !((EntityHumanoid) entityToLook).isDead())
				{
					//Check target is in set
					if (targetsTypes.contains(entityToLook.getClass()))
					{
						//Play a borking sound
						entity.getWorld().getSoundManager().playSoundEffect("sounds/sfx/zombie.ogg", entity.getLocation(), (float) (1.5 + Math.random() * 0.2), 1.5f);//.setPitch();
						entity.getWorld().getSoundManager().playSoundEffect("sounds/sfx/zombie.ogg", entity.getLocation(), (float) (1.5 + Math.random() * 0.2), 1.5f);//.setPitch();
						
						//Set new task
						setAiTask(new AiTaskAttackEntity((EntityHumanoid) entityToLook, 10f, currentTask, 1200, 15));
						return;
					}
				}
			}

		}
	}

	class AiTaskAttackEntity extends AiTaskGoAtEntity
	{
		final long attackCooldownMS;
		final float damage;

		long lastAttackMS = 0;

		public AiTaskAttackEntity(EntityHumanoid entity, float maxDistance, AI<EntityHumanoid>.AiTask previousTask, long attackCooldownMS, float damage)
		{
			super(entity, maxDistance, previousTask);

			this.attackCooldownMS = attackCooldownMS;
			this.damage = damage;

			this.entitySpeed = 0.05;
		}

		@Override
		public void execute()
		{
			super.execute();

			if (this.entityFollowed.getLocation().distanceTo(entity.getLocation()) < 1.5)
			{
				if (System.currentTimeMillis() - lastAttackMS > attackCooldownMS)
				{
					System.out.println("Attacking");
					entityFollowed.damage(entity, damage);
					lastAttackMS = System.currentTimeMillis();
				}
			}
		}
	}
}
