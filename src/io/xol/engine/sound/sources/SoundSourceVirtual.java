package io.xol.engine.sound.sources;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.server.propagation.VirtualServerSoundManager;

//(c) 2015-2017 XolioWare Interactive
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
	VirtualServerSoundManager virtualServerSoundManager;
	
	public SoundSourceVirtual(VirtualServerSoundManager virtualServerSoundManager, String soundEffect, float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, boolean buffered, float attStart, float attEnd)
	{
		super(x, y, z, loop, ambient, pitch, gain, attStart, attEnd);
		this.buffered = buffered;
		this.soundEffect = soundEffect;
		this.virtualServerSoundManager = virtualServerSoundManager;
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
	public SoundSource setPitch(float pitch)
	{
		super.setPitch(pitch);
		virtualServerSoundManager.updateSourceForEveryone(this, null);
		return this;
	}

	@Override
	public SoundSource setGain(float gain)
	{
		super.setGain(gain);
		virtualServerSoundManager.updateSourceForEveryone(this, null);
		return this;
	}

	@Override
	public SoundSource setPosition(float x, float y, float z)
	{
		super.setPosition(x, y, z);
		virtualServerSoundManager.updateSourceForEveryone(this, null);
		return this;
	}

	@Override
	public SoundSource setAttenuationStart(float start)
	{
		super.setAttenuationStart(start);
		virtualServerSoundManager.updateSourceForEveryone(this, null);
		return this;
	}

	@Override
	public SoundSource setAttenuationEnd(float end)
	{
		super.setAttenuationEnd(end);
		System.out.println("Set att end:"+end);
		virtualServerSoundManager.updateSourceForEveryone(this, null);
		return this;
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
