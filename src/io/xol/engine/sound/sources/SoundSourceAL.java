package io.xol.engine.sound.sources;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;

import static org.lwjgl.openal.EFX10.*;

import org.lwjgl.openal.AL10;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.concurrency.SimpleLock;
import io.xol.engine.sound.ALSoundManager;
import io.xol.engine.sound.SoundData;
import io.xol.engine.sound.SoundDataBuffered;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SoundSourceAL implements SoundSource
{
	SimpleLock lock = new SimpleLock();

	public long soundStartTime;
	public long internalID;
	public int alId;

	public float x, y, z;
	public float vx = 0f, vy = 0f, vz = 0f;
	public float pitch;
	public float gain;

	public boolean loop = false;
	public boolean isAmbient = false;

	public float start = 5f;
	public float end = 25f;

	boolean updateProperties = true;

	public SoundData soundData;
	
	SoundEffect soundEffect;

	public SoundSourceAL(SoundData data, float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.gain = gain;
		this.pitch = pitch;
		this.loop = loop;
		this.isAmbient = ambient;
		this.soundData = data;
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
		if (this.start != start)
			updateProperties = true;
		this.start = start;
		lock.unlock();
		return this;
	}

	@Override
	public SoundSource setAttenuationEnd(float end)
	{
		lock.lock();
		if (this.end != end)
			updateProperties = true;
		this.end = end;
		lock.unlock();
		return this;
	}

	@Override
	public SoundSource applyEffect(SoundEffect soundEffect)
	{
		this.soundEffect = soundEffect;
		return this;
	}
	
	public void play()
	{
		alId = alGenSources();
		if (soundData == null)
			ChunkStoriesLogger.getInstance().warning("A sound source was asked to play a null soundData !");
		else
		{
			if (soundData instanceof SoundDataBuffered)
			{
				SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
				//Upload the first two pages, the first one is set to be the first one we'll swap
				alSourceQueueBuffers(alId, sdb.uploadNextPage(alId));
				alSourceQueueBuffers(alId, sdb.uploadNextPage(alId));
			}
			else
				alSourcei(alId, AL_BUFFER, soundData.getBuffer());

			updateSource();
			alSourcePlay(alId);
			soundStartTime = System.currentTimeMillis();
			//alSource (alId, AL_VELOCITY, sourceVel     );
		}
	}

	@Override
	public void update(SoundManager manager)
	{
		//Update buffered sounds
		if (soundData instanceof SoundDataBuffered)
		{
			SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
			//Gets how many buffers we read entirely
			int elapsed = AL10.alGetSourcei(alId, AL_BUFFERS_PROCESSED);
			while (elapsed > 0)
			{
				//Get rid of them
				int removeMeh = AL10.alSourceUnqueueBuffers(alId);
				AL10.alDeleteBuffers(removeMeh);
				//Queue a new one
				alSourceQueueBuffers(alId, sdb.uploadNextPage(alId));
				elapsed--;
			}
		}
		ALSoundManager alManager = ((ALSoundManager) manager);
		if (isAmbient)
		{
			//To get rid of spatialization we tp the ambient sources to the listener
			x = alManager.x;
			y = alManager.y;
			z = alManager.z;
		}
		if(soundEffect != null)
			effectSlotId = alManager.getSlotForEffect(soundEffect);
		updateSource();
	}

	/**
	 * Returns wether the sound source is not active anymore
	 * 
	 * @return
	 */
	@Override
	public boolean isDonePlaying()
	{
		if (soundData == null)
			return true;
		return !loop && (System.currentTimeMillis() - soundStartTime > soundData.getLengthMs());
	}

	/**
	 * Removes and stops the SoundSource. In case this source was using an unique SoundData (ie streamed/buffered) it also deletes the said source and frees ressources.
	 */
	@Override
	public void destroy()
	{
		alSourceStop(alId);
		if (soundData instanceof SoundDataBuffered)
		{
			SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
			int elapsed = AL10.alGetSourcei(alId, AL_BUFFERS_PROCESSED);
			while (elapsed > 0)
			{
				int removeMeh = AL10.alSourceUnqueueBuffers(alId);
				AL10.alDeleteBuffers(removeMeh);
				elapsed--;
			}
			sdb.destroy();
		}
		//Set soundData to null to allow for garbage collection
		soundData = null;
		alDeleteSources(alId);
		/*if(efxSlot != -1)
			alDeleteAuxiliaryEffectSlots(efxSlot);*/
	}

	int effectSlotId = -1;
	
	private void updateSource()
	{
		alSource3f(alId, AL_POSITION, x, y, z);
		/*if (efxSlot == -1 && ALSoundManager.efxOn)
		{
			efxSlot = alGenAuxiliaryEffectSlots();
			alAuxiliaryEffectSloti(efxSlot, AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL_TRUE);

			if (reverbEffectSlot == -1)
			{
				reverbEffectSlot = alGenEffects();
				alEffecti(reverbEffectSlot, AL_EFFECT_TYPE, AL_EFFECT_REVERB);
				alEffectf(reverbEffectSlot, AL_REVERB_DENSITY, AL_REVERB_DEFAULT_DENSITY);
				alEffectf(reverbEffectSlot, AL_REVERB_DIFFUSION, AL_REVERB_DEFAULT_DIFFUSION);
				alEffectf(reverbEffectSlot, AL_REVERB_GAIN, AL_REVERB_DEFAULT_GAIN);
				alEffectf(reverbEffectSlot, AL_REVERB_GAINHF, AL_REVERB_DEFAULT_GAINHF);
				//alEffectf(reverbEffectSlot, AL_REVERB_GAINLF, pEFXEAXReverb.flGainLF);
				alEffectf(reverbEffectSlot, AL_REVERB_DECAY_TIME, AL_REVERB_DEFAULT_DECAY_TIME);
				alEffectf(reverbEffectSlot, AL_REVERB_DECAY_HFRATIO, AL_REVERB_DEFAULT_DECAY_HFRATIO);
				//alEffectf(reverbEffectSlot, AL_REVERB_DECAY_LFRATIO, AL_REVERB_DECAY_LFRATIO);
				alEffectf(reverbEffectSlot, AL_REVERB_REFLECTIONS_GAIN, AL_REVERB_DEFAULT_REFLECTIONS_GAIN);
				alEffectf(reverbEffectSlot, AL_REVERB_REFLECTIONS_DELAY, AL_REVERB_DEFAULT_REFLECTIONS_DELAY);
				//alEffectfv(reverbEffectSlot, AL_REVERB_REFLECTIONS_PAN, @pEFXEAXReverb.flReflectionsPan);
				alEffectf(reverbEffectSlot, AL_REVERB_LATE_REVERB_GAIN, AL_REVERB_DEFAULT_LATE_REVERB_GAIN);
				alEffectf(reverbEffectSlot, AL_REVERB_LATE_REVERB_DELAY, AL_REVERB_DEFAULT_LATE_REVERB_DELAY);
				//alEffectfv(reverbEffectSlot, AL_REVERB_LATE_REVERB_PAN, @pEFXEAXReverb.flLateReverbPan);
				//alEffectf(reverbEffectSlot, AL_REVERB_ECHO_TIME, pEFXEAXReverb.flEchoTime);
				//alEffectf(reverbEffectSlot, AL_REVERB_ECHO_DEPTH, pEFXEAXReverb.flEchoDepth);
				//alEffectf(reverbEffectSlot, AL_REVERB_MODULATION_TIME, pEFXEAXReverb.flModulationTime);
				//alEffectf(reverbEffectSlot, AL_REVERB_MODULATION_DEPTH, pEFXEAXReverb.flModulationDepth);
				alEffectf(reverbEffectSlot, AL_REVERB_AIR_ABSORPTION_GAINHF, AL_REVERB_DEFAULT_AIR_ABSORPTION_GAINHF);
				//alEffectf(reverbEffectSlot, AL_REVERB_HFREFERENCE, pEFXEAXReverb.flHFReference);
				//alEffectf(reverbEffectSlot, AL_REVERB_LFREFERENCE, pEFXEAXReverb.flLFReference);
				alEffectf(reverbEffectSlot, AL_REVERB_ROOM_ROLLOFF_FACTOR, AL_REVERB_DEFAULT_ROOM_ROLLOFF_FACTOR);
				alEffecti(reverbEffectSlot, AL_REVERB_DECAY_HFLIMIT, AL_REVERB_DEFAULT_DECAY_HFLIMIT);
			}
			
			alAuxiliaryEffectSloti(efxSlot, AL_EFFECTSLOT_EFFECT, reverbEffectSlot);
		}*/

		if (updateProperties)
		{
			alSourcef(alId, AL_PITCH, pitch);
			alSourcef(alId, AL_GAIN, gain);
			alSourcef(alId, AL_ROLLOFF_FACTOR, isAmbient ? 0f : 1f);
			alSourcef(alId, AL_REFERENCE_DISTANCE, start);
			alSourcef(alId, AL_MAX_DISTANCE, end);
			//System.out.println(efxSlot + ":"+reverbEffectSlot);
			if(effectSlotId != -1)
				alSource3i(alId, AL_AUXILIARY_SEND_FILTER, effectSlotId, 0, AL_FILTER_NULL);
			else
			    alSource3i(alId, AL_AUXILIARY_SEND_FILTER, AL_EFFECTSLOT_NULL, 0, AL_FILTER_NULL);
			//alSource3i(alId, AL_AUXILIARY_SEND_FILTER, efxSlot, 0, AL_FILTER_NULL);
			updateProperties = false;
		}
	}
}
