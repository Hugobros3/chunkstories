package io.xol.engine.sound.sources;

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.engine.sound.library.SoundsLibrary;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ALBufferedSoundSource extends ALSoundSource
{
	public ALBufferedSoundSource(String soundEffect, float x, float y, float z, boolean loop, boolean ambient, float pitch, float gain, float attStart, float attEnd) throws SoundEffectNotFoundException
	{
		super(x, y, z, loop, ambient, pitch, gain, attStart, attEnd);
		this.soundData = SoundsLibrary.obtainBufferedSample(soundEffect);
		if(soundData == null)
			throw new SoundEffectNotFoundException();
	}
}
