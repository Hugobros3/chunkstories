package io.xol.engine.sound.sources;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ALBufferedSoundSource extends ALSoundSource
{
	public ALBufferedSoundSource(String soundEffect, Vector3dc position, float pitch, float gain, float attStart, float attEnd) throws SoundEffectNotFoundException
	{
		super(Mode.STREAMED, position, pitch, gain, attStart, attEnd);
		this.soundData = SoundsLibrary.obtainBufferedSample(soundEffect);
		if(soundData == null)
			throw new SoundEffectNotFoundException();
	}
}
