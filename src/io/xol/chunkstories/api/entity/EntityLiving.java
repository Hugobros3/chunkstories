package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.entity.interfaces.EntityRotateable;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityLiving extends Entity, EntityRotateable, DamageCause
{
	public float getMaxHealth();
	
	public float getStartHealth();
	
	public void setHealth(float health);
	
	public float getHealth();
	
	/**
	 * Damages the entity. Overriding this method may allow the entity to resist better to certain types of damages
	 * @param cause
	 * @param damage
	 * @return Damage effectivly taken
	 */
	public float damage(DamageCause cause, float damage);
	
	/**
	 * Returns true if the entity is dead
	 * If an entity is dead it can't be interacted with anymore
	 * @return
	 */
	public boolean isDead();
	
}
