//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.sound.sources;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.sound.SoundSource;

/**
 * Server-side version of a soundSource
 */
public class SoundSourceVirtual extends SoundSourceAbstract
{
	public boolean stopped = false;
	String soundEffect;
	VirtualSoundManager virtualServerSoundManager;
	
	public SoundSourceVirtual(VirtualSoundManager virtualServerSoundManager, String soundEffect, Mode mode, Vector3dc position, float pitch, float gain, float attStart, float attEnd)
	{
		super(mode, position, pitch, gain, attStart, attEnd);
		this.soundEffect = soundEffect;
		this.virtualServerSoundManager = virtualServerSoundManager;
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
	public SoundSource setPosition(Vector3dc position) {
		super.setPosition(position);
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
