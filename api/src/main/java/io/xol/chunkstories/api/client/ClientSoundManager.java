package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.sound.SoundManager;

public interface ClientSoundManager extends SoundManager {

	void replicateServerSoundSource(String soundName, float x, float y, float z, boolean loop, boolean isAmbient,
			float pitch, float gain, float attenuationStart, float attenuationEnd, boolean buffered, long UUID);

}
