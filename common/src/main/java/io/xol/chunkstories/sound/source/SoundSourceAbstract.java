//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.source;

import java.util.Random;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.util.concurrency.SimpleLock;

public abstract class SoundSourceAbstract implements SoundSource
{
	protected long soundStartTime;
	protected long soundSourceUUID = -1;
	
	private final Mode mode;

	protected final Vector3d position;
	
	protected float pitch;
	protected float gain;
	//public boolean loop = false;
	//public boolean isAmbient = false;

	protected float attenuationStart = 1f;
	protected float attenuationEnd = 25f;

	SimpleLock lock = new SimpleLock();
	protected boolean updateProperties = true;

	static Random random = new Random();
	
	public SoundSourceAbstract(Mode mode, Vector3dc position, float pitch, float gain, float attStart, float attEnd)
	{
		soundSourceUUID = random.nextLong();
		
		this.mode = mode;
		
		if(position != null)
			this.position = new Vector3d(position);
		else
			this.position = null;
		
		
		this.gain = gain;
		this.pitch = pitch;
		
		this.attenuationStart = attStart;
		this.attenuationEnd = attEnd;
	}
	
	@Override
	public Mode getMode() {
		return mode;
	}

	@Override
	public long getUUID()
	{
		return soundSourceUUID;
	}

	public Vector3d getPosition() {
		return position;
	}

	public float getPitch() {
		return pitch;
	}

	public float getGain() {
		return gain;
	}

	public float getAttenuationStart() {
		return attenuationStart;
	}

	public float getAttenuationEnd() {
		return attenuationEnd;
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
	/*@Override
	public SoundSource setAmbient(boolean ambient)
	{
		lock.lock();
		if (isAmbient != ambient)
			updateProperties = true;
		this.isAmbient = ambient;
		lock.unlock();
		return this;
	}*/

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
		this.position.set(x, y, z);
		lock.unlock();
		return this;
	}
	
	@Override
	public SoundSource setPosition(Vector3dc position) {
		lock.lock();
		this.position.set(position);
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
