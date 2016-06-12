package io.xol.chunkstories.api.sound;

import java.nio.FloatBuffer;

import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class SoundManager
{
	// Abstract, API-Level class to define how the SoundManager should be used

	public abstract void setListenerPosition(float x, float y, float z, FloatBuffer rot);
	
	/**
	 * Overloads playSoundEffect with doubles instead of floats
	 */
	public SoundSource playSoundEffect(String soundEffect, double x, double y, double z, float pitch, float gain)
	{
		return playSoundEffect(soundEffect, (float)x, (float)y, (float)z, pitch, gain);
	}

	/**
	 * Overloads playSoundEffect with a vector3d instead of individual components
	 */
	public SoundSource playSoundEffect(String soundEffect, Vector3d location, float pitch, float gain)
	{
		return playSoundEffect(soundEffect, location.x, location.y, location.z, pitch, gain);
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
	public abstract SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain);

	/**
	 * Plays a soundEffect with no context in the world, for gui or other sounds that need no attenuation or position.
	 * @param soundEffect
	 * @return
	 */
	public abstract SoundSource playSoundEffect(String soundEffect);

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
	public abstract SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient);

	/**
	 * Stops all currently playing sounds matching this name
	 * @param soundEffect
	 */
	public abstract void stopAnySound(String soundEffect);

	/**
	 * Stops all currently playing sounds
	 * @param soundEffect
	 */
	public abstract void stopAnySound();

	/**
	 * Different OpenAL implementations means different limits to the number of concurrent effects possible.
	 * As it's kind of a chore to make one that works everytime we expose the inner workings to allow programmers to do what they want of it.
	 * @return The max number of OpenAL effect slots
	 */
	public abstract int getMaxEffectsSlots();
	
	/**
	 * Tells the SoundEngine to register this effect on this slot.
	 * @param effect See {@link SoundEffect}
	 * @return If it succeeded or not.
	 */
	public abstract boolean setEffectForSlot(int slot, SoundEffect effect);
	
	/**
	 * Internal to the engine
	 */
	public abstract void destroy();

	/**
	 * Internal to the engine
	 */
	public abstract void update();

	public void setListenerPosition(double d, double e, double f, FloatBuffer listenerOrientation)
	{
		setListenerPosition((float)d, (float)e, (float)f, listenerOrientation);
	}
}
