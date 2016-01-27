package io.xol.engine.sound;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class SoundData
{
	public abstract long getLengthMs();
	
	public abstract boolean loadedOk();
	
	public abstract int getBuffer();
	
	public abstract void destroy();

	public abstract String getName();
}
