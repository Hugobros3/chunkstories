//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.sound;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.sound.ogg.SoundDataOggSample;
import io.xol.chunkstories.sound.ogg.SoundDataOggStream;

public class SoundsLibrary {
	private final ClientImplementation client;
	static Map<String, SoundData> soundsData = new HashMap<>();

	public SoundsLibrary(ClientImplementation client) {
		this.client = client;
	}

	public SoundData obtainOggSample(String soundEffect) {
		SoundDataOggSample sd = new SoundDataOggSample(client.getContent().getAsset(soundEffect));
		sd.name = soundEffect;
		if (sd.loadedOk())
			return sd;
		return null;
	}

	public SoundData obtainSample(String soundEffect) {
		if (soundEffect == null)
			return null;

		if (!soundEffect.startsWith("./"))
			soundEffect = "./" + soundEffect;

		SoundData sd = soundsData.get(soundEffect);
		if (sd != null)
			return sd;
		if (soundEffect.endsWith(".ogg")) {
			sd = obtainOggSample(soundEffect);
		} else
			return null;
		if (sd == null)
			return null;
		soundsData.put(soundEffect, sd);
		return sd;
	}

	public void clean() {
		for (SoundData sd : soundsData.values()) {
			sd.destroy();
		}
		soundsData.clear();
	}

	/**
	 * This methods returns an unique SoundData used exclusivly for a specific
	 * SoundSourceBuffered
	 * 
	 * @param musicName
	 * @return
	 */
	public SoundData obtainBufferedSample(String musicName) {
		if (!musicName.startsWith("./"))
			musicName = "./" + musicName;

		SoundDataBuffered sd;
		if (musicName.endsWith(".ogg")) {
			sd = obtainOggStream(musicName);
		} else
			return null;
		return sd;
	}

	private SoundDataBuffered obtainOggStream(String musicName) {
		SoundDataOggStream sd;

		sd = new SoundDataOggStream(client.getContent().getAsset(musicName).read());
		sd.name = musicName;
		if (sd.loadedOk())
			return sd;

		return null;
	}

}
