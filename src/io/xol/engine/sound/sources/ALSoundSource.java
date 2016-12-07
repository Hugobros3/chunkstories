package io.xol.engine.sound.sources;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;

import static org.lwjgl.openal.EFX10.*;

import org.lwjgl.openal.AL10;

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.sound.ALSoundManager;
import io.xol.engine.sound.SoundData;
import io.xol.engine.sound.SoundDataBuffered;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ALSoundSource extends SoundSourceAbstract
{
	public int openAlSourceId;

	public SoundData soundData;

	SoundEffect effect;

	ALSoundSource(float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, float attStart, float attEnd)
	{
		super(x, y, z, loop, ambient, pitch, gain, attStart, attEnd);
	}

	public void setUUID(long uUID)
	{
		this.soundSourceUUID = uUID;
	}

	public ALSoundSource(String soundEffect, float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, float attStart, float attEnd) throws SoundEffectNotFoundException
	{
		this(x, y, z, loop, ambient, pitch, gain, attStart, attEnd);

		this.soundData = SoundsLibrary.obtainSample(soundEffect);
		if (soundData == null)
			throw new SoundEffectNotFoundException();
	}

	@Override
	public SoundSource applyEffect(SoundEffect soundEffect)
	{
		this.effect = soundEffect;
		return this;
	}

	public void play()
	{
		openAlSourceId = alGenSources();
		if (soundData == null)
			ChunkStoriesLogger.getInstance().warning("A sound source was asked to play a null soundData !");
		else
		{
			if (soundData instanceof SoundDataBuffered)
			{
				SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
				//Upload the first two pages, the first one is set to be the first one we'll swap
				alSourceQueueBuffers(openAlSourceId, sdb.uploadNextPage(openAlSourceId));
				alSourceQueueBuffers(openAlSourceId, sdb.uploadNextPage(openAlSourceId));
			}
			else
				alSourcei(openAlSourceId, AL_BUFFER, soundData.getBuffer());

			updateSource();
			alSourcePlay(openAlSourceId);
			soundStartTime = System.currentTimeMillis();
			//alSource (alId, AL_VELOCITY, sourceVel     );
		}
	}

	public void update(SoundManager manager)
	{
		//Update buffered sounds
		if (soundData instanceof SoundDataBuffered)
		{
			SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
			//Gets how many buffers we read entirely
			int elapsed = AL10.alGetSourcei(openAlSourceId, AL_BUFFERS_PROCESSED);
			while (elapsed > 0)
			{
				//Get rid of them
				int removeMeh = AL10.alSourceUnqueueBuffers(openAlSourceId);
				AL10.alDeleteBuffers(removeMeh);
				//Queue a new one
				alSourceQueueBuffers(openAlSourceId, sdb.uploadNextPage(openAlSourceId));
				elapsed--;
			}
		}
		ALSoundManager alManager = ((ALSoundManager) manager);
		/*if (isAmbient)
		{
			//To get rid of spatialization we tp the ambient sources to the listener
			x = alManager.x;
			y = alManager.y;
			z = alManager.z;
		}*/
		if (effect != null)
			effectSlotId = alManager.getSlotForEffect(effect);
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
	public void stop()
	{
		alSourceStop(openAlSourceId);
		if (soundData instanceof SoundDataBuffered)
		{
			SoundDataBuffered sdb = ((SoundDataBuffered) soundData);
			int elapsed = AL10.alGetSourcei(openAlSourceId, AL_BUFFERS_PROCESSED);
			while (elapsed > 0)
			{
				int removeMeh = AL10.alSourceUnqueueBuffers(openAlSourceId);
				AL10.alDeleteBuffers(removeMeh);
				elapsed--;
			}
			sdb.destroy();
		}
		//Set soundData to null to allow for garbage collection
		soundData = null;
		alDeleteSources(openAlSourceId);
		/*if(efxSlot != -1)
			alDeleteAuxiliaryEffectSlots(efxSlot);*/
	}

	int effectSlotId = -1;

	private void updateSource()
	{
		alSource3f(openAlSourceId, AL_POSITION, x, y, z);
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

		lock.lock();
		if (updateProperties)
		{
			alSourcef(openAlSourceId, AL_PITCH, pitch);
			alSourcef(openAlSourceId, AL_GAIN, gain);
			//alSourcef(openAlSourceId, AL_ROLLOFF_FACTOR, isAmbient ? 0f : 1f);

			if (isAmbient)
			{
				alSourcei(openAlSourceId, AL_SOURCE_RELATIVE, AL_TRUE);
				alSource3f(openAlSourceId, AL_POSITION, 0.0f, 0.0f, 0.0f);
				alSource3f(openAlSourceId, AL_VELOCITY, 0.0f, 0.0f, 0.0f);
			}
			alSourcei(openAlSourceId, AL_ROLLOFF_FACTOR, isAmbient ? 0 : 1);

			alSourcef(openAlSourceId, AL_REFERENCE_DISTANCE, attenuationStart);
			alSourcef(openAlSourceId, AL_MAX_DISTANCE, attenuationEnd);
			//System.out.println(efxSlot + ":"+reverbEffectSlot);
			if (effectSlotId != -1)
				alSource3i(openAlSourceId, AL_AUXILIARY_SEND_FILTER, effectSlotId, 0, AL_FILTER_NULL);
			else
				alSource3i(openAlSourceId, AL_AUXILIARY_SEND_FILTER, AL_EFFECTSLOT_NULL, 0, AL_FILTER_NULL);
			//alSource3i(alId, AL_AUXILIARY_SEND_FILTER, efxSlot, 0, AL_FILTER_NULL);
			updateProperties = false;
		}
		lock.unlock();
	}

	@Override
	public String getSoundName()
	{
		return soundData.getName();
	}
}
