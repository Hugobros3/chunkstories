package io.xol.engine.sound.sources;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.sound.SoundSource;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DummySoundSource implements SoundSource
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

	@Override
	public Mode getMode() {
		// TODO Auto-generated method stub
		return Mode.NORMAL;
	}

	@Override
	public SoundSource setPosition(Vector3dc location) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getPitch() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getGain() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getAttenuationStart() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getAttenuationEnd() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector3dc getPosition() {
		// TODO Auto-generated method stub
		return null;
	}

}
