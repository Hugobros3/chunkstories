package io.xol.engine.sound;

import java.nio.FloatBuffer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class SoundManager
{
	// XolioEngine SoundEngine
	
	public SoundManager()
	{
		
	}

	public abstract void destroy();

	public abstract void update();

	public abstract void setListenerPosition(float x, float y, float z, FloatBuffer rot);
	
	public void playSoundEffect(String soundEffect, double x, double y, double z, float pitch, float gain)
	{
		playSoundEffect(soundEffect, (float)x, (float)y, (float)z, pitch, gain);
	}

	public abstract void playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain);

	public abstract void stopAnySound(String soundEffect);

	abstract int removeUnusedSources();

	abstract public void playSoundEffect(String soundEffect);
}
