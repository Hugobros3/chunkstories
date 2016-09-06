package io.xol.engine.sound;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundSource;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DummySound implements SoundSource
{

	@Override
	public String getSoundName()
	{
		// TODO Auto-generated method stub
		return "dummy";
	}

	@Override
	public long getUUID()
	{
		return -1;
	}

	@Override
	public SoundSource setPitch(float pitch)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource setAmbient(boolean ambient)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource setGain(float gain)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource setAttenuationStart(float start)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource setAttenuationEnd(float end)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource setPosition(float x, float y, float z)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SoundSource applyEffect(SoundEffect soundEffect)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDonePlaying()
	{
		// TODO Auto-generated method stub
		return true;
	}

}
