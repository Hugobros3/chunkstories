package io.xol.chunkstories.api.net;

import io.xol.chunkstories.api.entity.components.Subscriber;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The remote server interface is used as a virtual subscriber ( the server being the 'subscriber' ) to the controlled entity of the client
 * so it receives updates from the client.
 */
public interface RemoteServer extends Subscriber, PacketDestinator
{

}
