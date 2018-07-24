//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input;

import io.xol.chunkstories.api.input.Input;

/**
 * LWJGL allows thread-safe accessing of keyboard and mouse objects, but it's
 * much better to poll them in the main thread and cache the results so the
 * other threads can't be stalled by rendering.
 */
public interface Pollable extends Input {
	public void updateStatus();
}
