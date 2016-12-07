package io.xol.engine.sound.sources;

import java.util.Random;

import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.engine.concurrency.SimpleLock;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class SoundSourceAbstract implements SoundSource
{
	public long soundStartTime;
	public long soundSourceUUID = -1;
	public float x;
	public float y;
	public float z;
	public float pitch;
	public float gain;
	public boolean loop = false;
	public boolean isAmbient = false;

	public float attenuationStart = 1f;
	public float attenuationEnd = 25f;

	SimpleLock lock = new SimpleLock();
	boolean updateProperties = true;

	static Random random = new Random();
	
	SoundSourceAbstract()
	{
		soundSourceUUID = random.nextLong();
	}
	
	public long getUUID()
	{
		return soundSourceUUID;
	}
	
	public SoundSourceAbstract(float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, float attStart, float attEnd)
	{
		this();
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.gain = gain;
		this.pitch = pitch;
		this.loop = loop;
		this.isAmbient = ambient;
		
		this.attenuationStart = attStart;
		this.attenuationEnd = attEnd;
	}

	/**
	 * Sets the pitch to a specific source
	 * 
	 * @param pitch
	 * @return
	 */
	@Override
	public SoundSource setPitch(float pitch)
	{
		lock.lock();
		if (this.pitch != pitch)
			updateProperties = true;
		this.pitch = pitch;
		lock.unlock();
		return this;
	}

	/**
	 * Ambient SoundSources have the special property of always being "on" the listener, thus never getting attenuated and not suffering from directional distorsions.
	 * 
	 * @param ambient
	 * @return
	 */
	@Override
	public SoundSource setAmbient(boolean ambient)
	{
		lock.lock();
		if (isAmbient != ambient)
			updateProperties = true;
		this.isAmbient = ambient;
		lock.unlock();
		return this;
	}

	/**
	 * Sets the gain of the source
	 * 
	 * @param gain
	 * @return
	 */
	@Override
	public SoundSource setGain(float gain)
	{
		lock.lock();
		if (this.gain != gain)
			updateProperties = true;
		this.gain = gain;
		lock.unlock();
		return this;
	}

	/**
	 * Sets the source position in the World
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return The working SoundSource
	 */
	@Override
	public SoundSource setPosition(float x, float y, float z)
	{
		lock.lock();
		this.x = x;
		this.y = y;
		this.z = z;
		lock.unlock();
		return this;
	}

	@Override
	public SoundSource setAttenuationStart(float start)
	{
		lock.lock();
		if (this.attenuationStart != start)
			updateProperties = true;
		this.attenuationStart = start;
		lock.unlock();
		return this;
	}

	@Override
	public SoundSource setAttenuationEnd(float end)
	{
		lock.lock();
		if (this.attenuationEnd != end)
			updateProperties = true;
		this.attenuationEnd = end;
		lock.unlock();
		
		return this;
	}
}
