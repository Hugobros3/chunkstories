//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound.source;

import org.joml.Vector3dc;

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.sound.SoundsLibrary;

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
