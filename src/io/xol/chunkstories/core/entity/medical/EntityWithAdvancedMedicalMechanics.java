package io.xol.chunkstories.core.entity.medical;

import io.xol.chunkstories.api.entity.EntityLiving;

public interface EntityWithAdvancedMedicalMechanics extends EntityLiving
{
	public AdvancedMedicalComponent getAdvancedMedicalComponent();
}
