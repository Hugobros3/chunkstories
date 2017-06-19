package io.xol.chunkstories.api.entity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface DamageCause
{
	public String getName();
	
	/**
	 * How many milliseconds should the target be set invulnerable after an attack
	 */
	public default long getCooldownInMs()
	{
		return 0;
	}
}
