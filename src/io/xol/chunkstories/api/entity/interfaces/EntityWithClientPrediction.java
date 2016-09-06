package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.Location;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityWithClientPrediction
{
	/**
	 * Called each tick() on the client, on non-controlled entities
	 */
	public void tickClientPrediction();
	
	/**
	 * Used to interpolate positions in multiplayer, as well as rendering the EntityControllable at the same location it was when the camera object was called
	 */
	public Location getPredictedLocation();
}
