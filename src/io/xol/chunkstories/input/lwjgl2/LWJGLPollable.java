package io.xol.chunkstories.input.lwjgl2;

import io.xol.chunkstories.api.input.Input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * LWJGL allows thread-safe accessing of keyboard and mouse objects, but it's much better to poll them in the main thread and cache the results so the other threads can't be stalled by rendering.
 */
public interface LWJGLPollable extends Input
{
	public void updateStatus();
}
