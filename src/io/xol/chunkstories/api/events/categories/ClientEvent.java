package io.xol.chunkstories.api.events.categories;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.client.Client;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes events occuring client-side
 * 
 * @author Hugo
 */
public interface ClientEvent
{
	public default ClientInterface getClient()
	{
		return Client.getInstance();
	}
}
