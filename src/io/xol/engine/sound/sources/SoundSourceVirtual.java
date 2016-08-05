package io.xol.engine.sound.sources;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundSource;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Server-side version of a soundSource
 */
public class SoundSourceVirtual extends SoundSourceAbstract
{
	public boolean stopped = false;
	public boolean buffered;
	String soundEffect;
	
	public SoundSourceVirtual(String soundEffect, float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, boolean buffered)
	{
		super(x, y, z, loop, ambient, pitch, gain);
		this.buffered = buffered;
		this.soundEffect = soundEffect;
	}

	@Override
	public SoundSource applyEffect(SoundEffect soundEffect)
	{
		return null;
	}

	@Override
	public void stop()
	{
		stopped = true;
	}

	@Override
	public boolean isDonePlaying()
	{
		return stopped;
	}

	@Override
	public String getSoundName()
	{
		return soundEffect;
	}
}
