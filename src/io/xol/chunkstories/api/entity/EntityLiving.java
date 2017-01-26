package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.entity.interfaces.EntityAnimated;
import io.xol.chunkstories.api.entity.interfaces.EntityRotateable;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.EntityRenderable;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityLiving extends Entity, EntityRenderable, EntityRotateable, EntityAnimated, DamageCause
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
	public float damage(DamageCause damageCause, float damage);
	
	public float damage(DamageCause damageCause, HitBox damageZone, float damage);
	
	public interface HitBox {

		public void draw(RenderingInterface renderingInterface);
		
		public Vector3dm lineIntersection(Vector3dm lineStart, Vector3dm lineDirection);

		public String getName();
	}
	
	public HitBox[] getHitBoxes();
	
	/**
	 * Returns null if this entity was never hurt, the last offender if it did
	 */
	public DamageCause getLastDamageCause();
	
	/**
	 * Returns true if the entity is dead
	 * If an entity is dead it can't be interacted with anymore
	 * @return
	 */
	public boolean isDead();
	
}
