//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.sound;



public abstract class SoundData
{
	public abstract long getLengthMs();
	
	public abstract boolean loadedOk();
	
	public abstract int getBuffer();
	
	public abstract void destroy();

	public abstract String getName();
}
