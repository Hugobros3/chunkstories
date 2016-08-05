package io.xol.chunkstories.api.sound;

import java.nio.FloatBuffer;
import java.util.Iterator;

import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public interface SoundManager
{
	// Abstract, API-Level class to define how the SoundManager should be used
	
	/**
	 * Overloads playSoundEffect with doubles instead of floats
	 */
	public default SoundSource playSoundEffect(String soundEffect, double x, double y, double z, float pitch, float gain)
	{
		return playSoundEffect(soundEffect, (float)x, (float)y, (float)z, pitch, gain);
	}

	/**
	 * Overloads playSoundEffect with a vector3d instead of individual components
	 */
	public default SoundSource playSoundEffect(String soundEffect, Vector3d location, float pitch, float gain)
	{
		return playSoundEffect(soundEffect, location.getX(), location.getY(), location.getZ(), pitch, gain);
	}

	/**
	 * Plays a soundEffect in the world
	 * @param soundEffect The name of the soundEffect, has to include the full path from %moddir%/sounds/ and the extension.
	 * @param x Position in the world, X
	 * @param y '' y
	 * @param z '' z
	 * @param pitch The pitch of the sound, keep it between 0.5 - 2.0 for best results
	 * @param gain The gain of the sound, ie volume
	 * @return
	 */
	public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain);

	/**
	 * Plays a soundEffect with no context in the world, for gui or other sounds that need no attenuation or position.
	 * @param soundEffect
	 * @return
	 */
	public SoundSource playSoundEffect(String soundEffect);

	/**
	 * Plays streamed music or long files ( > 15s ) where playSoundEffect fails
	 * @param musicName The name of the music, has to include the full path from %moddir%/sounds/ and the extension.
	 * @param x Position in the world, X
	 * @param y '' y
	 * @param z '' z
	  * @param pitch The pitch of the sound, keep it between 0.5 - 2.0 for best results
	 * @param gain The gain of the sound, ie volume
	 * @param ambient Wether or not the music should be part of the game world or be like playSoundEffect(sfx) 
	 * @return
	 */
	public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient);

	public default SoundSource getSoundSourceByUUID(long UUID)
	{
		Iterator<SoundSource> i = getAllPlayingSounds();
		while(i.hasNext())
		{
			SoundSource s = i.next();
			if(s.getUUID() == UUID)
				return s;
		}
		return null;
	}
	
	/**
	 * Stops all currently playing sounds matching this name
	 * @param soundEffect
	 */
	public void stopAnySound(String soundEffect);

	/**
	 * Stops all currently playing sounds
	 * @param effect
	 */
	public void stopAnySound();

	public Iterator<SoundSource> getAllPlayingSounds();
	
	/**
	 * Different OpenAL implementations means different limits to the number of concurrent effects possible.
	 * As it's kind of a chore to make one that works everytime we expose the inner workings to allow programmers to do what they want of it.
	 * @return The max number of OpenAL effect slots
	 */
	public int getMaxEffectsSlots();
	
	/**
	 * Tells the SoundEngine to register this effect on this slot.
	 * @param effect See {@link SoundEffect}
	 * @return If it succeeded or not.
	 */
	public abstract boolean setEffectForSlot(int slot, SoundEffect effect);

	public abstract void setListenerPosition(float x, float y, float z, FloatBuffer rot);
	
	public default void setListenerPosition(double d, double e, double f, FloatBuffer listenerOrientation)
	{
		setListenerPosition((float)d, (float)e, (float)f, listenerOrientation);
	}
}
