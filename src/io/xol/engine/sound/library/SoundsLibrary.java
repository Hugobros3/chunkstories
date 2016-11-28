package io.xol.engine.sound.library;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.content.ModsManager;
import io.xol.engine.sound.SoundData;
import io.xol.engine.sound.SoundDataBuffered;
import io.xol.engine.sound.ogg.SoundDataOggSample;
import io.xol.engine.sound.ogg.SoundDataOggStream;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SoundsLibrary
{
	// Internal class that stores the sounds
	
	static Map<String, SoundData> soundsData = new HashMap<String, SoundData>();
	
	public static SoundData obtainOggSample(String soundEffect)
	{
		SoundDataOggSample sd = new SoundDataOggSample(ModsManager.getAsset("./"+soundEffect));
		sd.name = soundEffect;
		if(sd.loadedOk())
			return sd;
		return null;
	}

	public static SoundData obtainSample(String soundEffect)
	{
		if(soundEffect == null)
			return null;
		
		SoundData sd = soundsData.get(soundEffect);
		if(sd != null)
			return sd;
		if (soundEffect.endsWith(".ogg"))
		{
			sd = obtainOggSample(soundEffect);
		}
		else
			return null;
		if(sd == null)
			return null;
		soundsData.put(soundEffect, sd);
		return sd;
	}
	
	public static void clean()
	{
		for(SoundData sd : soundsData.values())
		{
			sd.destroy();
		}
		soundsData.clear();
	}

	/**
	 * This methods returns an unique SoundData used exclusivly for a specific SoundSourceBuffered
	 * @param musicName
	 * @return
	 */
	public static SoundData obtainBufferedSample(String musicName)
	{
		SoundDataBuffered sd;
		if (musicName.endsWith(".ogg"))
		{
			sd = obtainOggStream(musicName);
		}
		else
			return null;
		return sd;
	}

	private static SoundDataBuffered obtainOggStream(String musicName)
	{
		SoundDataOggStream sd;
		
		sd = new SoundDataOggStream(ModsManager.getAsset("./"+musicName).read());
		sd.name = musicName;
		if(sd.loadedOk())
			return sd;
		
		return null;
	}
	
}
