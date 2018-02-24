//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.sound.sources;

import static org.lwjgl.openal.AL10.*;
import org.joml.Vector3dc;

//TODO EFX LWJGL3
//import static org.lwjgl.openal.EFX10.*;

import org.lwjgl.openal.AL10;

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.engine.sound.ALSoundManager;
import io.xol.engine.sound.SoundData;
import io.xol.engine.sound.SoundDataBuffered;
import io.xol.engine.sound.library.SoundsLibrary;



public class ALSoundSource extends SoundSourceAbstract
{
	public int openAlSourceId;

	public SoundData soundData;

	//SoundEffect effect;

	ALSoundSource(Mode mode, Vector3dc position, float pitch, float gain, float attStart, float attEnd)
	{
		super(mode, position, pitch, gain, attStart, attEnd);
	}

	public void setUUID(long uUID)
	{
		this.soundSourceUUID = uUID;
	}

	public ALSoundSource(String soundEffect, Mode mode, Vector3dc position, float pitch, float gain, float attStart, float attEnd) throws SoundEffectNotFoundException
	{
		this(mode, position, pitch, gain, attStart, attEnd);

		this.soundData = SoundsLibrary.obtainSample(soundEffect);
		if (soundData == null)
			throw new SoundEffectNotFoundException();
	}

	public void play()
	{
		openAlSourceId = alGenSources();
		if (soundData == null)
			ALSoundManager.logger.warn("A sound source was asked to play a null soundData !");
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
				alDeleteBuffers(removeMeh);
				//Queue a new one
				alSourceQueueBuffers(openAlSourceId, sdb.uploadNextPage(openAlSourceId));
				elapsed--;
			}
		}
		
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
		return !(getMode() == Mode.LOOPED) && (System.currentTimeMillis() - soundStartTime > soundData.getLengthMs());
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
		lock.lock();
		
		if(position != null)
			alSource3f(openAlSourceId, AL_POSITION, (float)position.x, (float)position.y, (float)position.z);
		
		if (updateProperties)
		{
			alSourcef(openAlSourceId, AL_PITCH, pitch);
			alSourcef(openAlSourceId, AL_GAIN, gain);

			boolean isAmbient = this.position == null;
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
			
			//TODO EFX LWJGL3
			/*if (effectSlotId != -1)
				alSource3i(openAlSourceId, AL_AUXILIARY_SEND_FILTER, effectSlotId, 0, AL_FILTER_NULL);
			else
				alSource3i(openAlSourceId, AL_AUXILIARY_SEND_FILTER, AL_EFFECTSLOT_NULL, 0, AL_FILTER_NULL);*/
			
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
