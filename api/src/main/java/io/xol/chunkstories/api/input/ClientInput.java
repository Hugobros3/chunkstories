package io.xol.chunkstories.api.input;

import io.xol.chunkstories.api.client.ClientInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Clientside input, presses are replicated on the server but they use VirtualInputs instead of these */
public interface ClientInput extends Input {
	public ClientInterface getClient();
}
