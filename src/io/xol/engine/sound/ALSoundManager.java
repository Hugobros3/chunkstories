package io.xol.engine.sound;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;

import io.xol.engine.sound.ogg.SoundDataOggSample;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ALSoundManager extends SoundManager
{
	protected Queue<SoundSource> playingSoundSources = new ConcurrentLinkedQueue<SoundSource>();
	
	Random rng;
	
	public ALSoundManager()
	{
		rng = new Random();
		try
		{
			AL.create();
			System.out.println("OpenAL context successfully created : "+AL.getContext().toString());
		}
		catch (LWJGLException e)
		{
			System.out.println("Failed to start sound system !");
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
		    public void run ()
		    {
		        AL.destroy();
		    }
		});
	}
	
	@Override
	public void destroy()
	{
		for(SoundSource ss : playingSoundSources)
			ss.onRemove();
		AL.destroy();
	}

	@Override
	public void update()
	{
		Iterator<SoundSource> i = playingSoundSources.iterator();
		while(i.hasNext())
		{
			SoundSource soundSource = i.next();
			if(soundSource.isDonePlaying())
			{
				System.out.println("Removed sound source "+soundSource+" id"+soundSource.internalID+" for being inactive.");
				i.remove();
			}
		}
	}

	@Override
	public void setListenerPosition(float x, float y, float z, FloatBuffer rot)
	{
		// TODO Auto-generated method stub

	}

	private void addSoundSource(SoundSource soundSource)
	{
		soundSource.internalID = rng.nextLong();
	}
	
	@Override
	public void playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain)
	{
		if(soundEffect.endsWith(".ogg"))
		{
			addSoundSource(new SoundSource(new SoundDataOggSample(new File("res/sound/"+soundEffect)), x, y, z, false, pitch, gain));
		}
	}

	@Override
	public void stopAnySound(String sfx)
	{
		// TODO Auto-generated method stub

	}

	@Override
	int removeUnusedSources()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void playSoundEffect(String soundEffect)
	{
		playSoundEffect(soundEffect, 0, 0, 0, 1, 1);
	}

}
