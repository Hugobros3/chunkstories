package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.Subscriber;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface WorldNetworked
{
	/**
	 * Temp
	 */
	public void processIncommingPackets();

	/**
	 * Plays a soundEffect to all clients except once, typical use if sounds played locally by a player that can't suffer any lag for him
	 * but still need others to hear it as well
	 */
	//public void playSoundEffectNetworked(String soundEffect, Location location, float pitch, float gain, Subscriber subscriberNotToPlaySoundFor);
}
