package io.xol.chunkstories.core.entity.components;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponentGenericFloatValue;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentFoodLevel extends EntityComponentGenericFloatValue
{
	public EntityComponentFoodLevel(Entity entity, float defaultValue)
	{
		super(entity, defaultValue);
	}

	public static DamageCause HUNGER_DAMAGE_CAUSE = new DamageCause() {

		@Override
		public String getName()
		{
			return "Hunger";
		}
		
	};
}
