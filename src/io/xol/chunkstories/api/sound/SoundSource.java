package io.xol.chunkstories.api.sound;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface SoundSource
{
	/**
	 * Sets the pitch to a specific source
	 * 
	 * @param pitch
	 * @return The working SoundSource
	 */
	public SoundSource setPitch(float pitch);

	/**
	 * Ambient SoundSources have the special property of always being "on" the listener, thus never getting attenuated and not suffering from directional distorsions.
	 * 
	 * @param ambient
	 * @return The working SoundSource
	 */
	public SoundSource setAmbient(boolean ambient);

	/**
	 * Sets the gain of the source
	 * 
	 * @param gain
	 * @return The working SoundSource
	 */
	public SoundSource setGain(float gain);

	/**
	 * Used to control the sound attenuation, gain = (1 - isAmbient)*(distance - start)/(max - start)
	 * @param start
	 * @return The working SoundSource
	 */
	public SoundSource setAttenuationStart(float start);
	
	/**
	 * Used to control the sound attenuation, gain = (1 - isAmbient)*(distance - start)/(max - start)
	 * @param end
	 * @return The working SoundSource
	 */
	public SoundSource setAttenuationEnd(float end);
	
	/**
	 * Sets the source position in the World
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return The working SoundSource
	 */
	public SoundSource setPosition(float x, float y, float z);

	/**
	 * Sets the effect applied to this source
	 * @param soundEffect A registered SoundEffect ( that is, one that went throught SoundManager's setEffectForSlot() )
	 * @return
	 */
	public SoundSource applyEffect(SoundEffect soundEffect);
	
	/**
	 * Removes and stops the SoundSource. In case this source was using an unique SoundData (ie streamed/buffered) it also deletes the said source and frees ressources.
	 */
	void destroy();

	/**
	 * Internal to the engine, updates the attributes in the AL context thread.
	 */

	void update(SoundManager manager);

	/**
	 * Returns wether the sound source is not active anymore
	 * 
	 * @return
	 */
	boolean isDonePlaying();

}
