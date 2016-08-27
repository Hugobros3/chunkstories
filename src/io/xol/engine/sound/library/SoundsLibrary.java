package io.xol.engine.sound.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

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
		SoundDataOggSample sd = new SoundDataOggSample(new File("res/" + soundEffect));
		sd.name = soundEffect;
		if(sd.loadedOk())
			return sd;
		return null;
	}

	public static SoundData obtainSample(String soundEffect)
	{
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
		try
		{
			sd = new SoundDataOggStream(new FileInputStream(new File("res/" + musicName)));
			sd.name = musicName;
			if(sd.loadedOk())
				return sd;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
}
