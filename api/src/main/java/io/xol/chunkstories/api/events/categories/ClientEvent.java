package io.xol.chunkstories.api.events.categories;

import io.xol.chunkstories.api.client.ClientInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes events occuring client-side
 */
public interface ClientEvent
{
	public ClientInterface getClient();
}
