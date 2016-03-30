package io.xol.engine.sound;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.EFX10.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.EFXUtil;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.sound.library.SoundsLibrary;
import io.xol.engine.sound.ogg.SoundDataOggSample;
import io.xol.engine.sound.sources.SoundSourceAL;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ALSoundManager extends SoundManager
{
	protected Queue<SoundSourceAL> playingSoundSources = new ConcurrentLinkedQueue<SoundSourceAL>();
	Random rng;
	
	Thread contextThread;
	// Are we allowed to use EFX effects
	public static boolean efxOn = false;

	int[] auxEffectsSlotsId;
	SoundEffect[] auxEffectsSlots;

	public ALSoundManager()
	{
		rng = new Random();
		try
		{
			AL.create();
			alDistanceModel(AL_LINEAR_DISTANCE_CLAMPED);
			String alVersion = alGetString(AL_VERSION);
			String alExtensions = alGetString(AL_EXTENSIONS);
			contextThread = Thread.currentThread();
			ChunkStoriesLogger.getInstance().info("OpenAL context successfully created, version = "+alVersion);
			ChunkStoriesLogger.getInstance().info("OpenAL Extensions avaible : "+alExtensions);
			efxOn = EFXUtil.isEfxSupported();
			ChunkStoriesLogger.getInstance().info("EFX extension support : "+(efxOn ? "yes" : "no"));
			if(efxOn)
			{
				//Reset error
				alGetError();
				List<Integer> auxSlotsIds = new ArrayList<Integer>();
				while(true)
				{
					int generated_id = alGenAuxiliaryEffectSlots();
					int error = alGetError();
					if(error != AL_NO_ERROR)
						break;
					auxSlotsIds.add(generated_id);
				}
				auxEffectsSlotsId = new int[auxSlotsIds.size()];
				int j = 0;
				for(int i : auxSlotsIds)
				{
					auxEffectsSlotsId[j] = i;
					j++;
				}
				auxEffectsSlots = new SoundEffect[auxSlotsIds.size()];
				ChunkStoriesLogger.getInstance().info(auxEffectsSlots.length+" avaible auxiliary effects slots.");
			}
		}
		catch (LWJGLException e)
		{
			System.out.println("Failed to start sound system !");
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				AL.destroy();
				System.out.println("OpenAL context successfully destroyed.");
			}
		});
	}

	@Override
	public void destroy()
	{
		for (SoundSource ss : playingSoundSources)
			ss.destroy();
		AL.destroy();
	}

	@Override
	public void update()
	{
		int result;
		if((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error at iter :"+SoundDataOggSample.getALErrorString(result));
		removeUnusedSources();
		Iterator<SoundSourceAL> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			SoundSourceAL soundSource = i.next();
			soundSource.update(this);
		}
	}
	
	public float x, y, z;

	@Override
	public void setListenerPosition(float x, float y, float z, FloatBuffer rot)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		FloatBuffer posScratch = BufferUtils.createFloatBuffer(3).put(new float[] { x, y, z });
		posScratch.flip();
		alListener(AL_POSITION, posScratch);
		//AL10.alListener(AL10.AL_VELOCITY, xxx);
		alListener(AL_ORIENTATION, rot);
		//FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });
	}

	long countTo9223372036854775808 = 0L;
	
	private void addSoundSource(SoundSourceAL soundSource)
	{
		countTo9223372036854775808++;
		soundSource.internalID = countTo9223372036854775808;
		playingSoundSources.add(soundSource);
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain)
	{
		SoundData data = SoundsLibrary.obtainSample(soundEffect);
		if (data != null)
		{
			SoundSourceAL ss = new SoundSourceAL(data, x, y, z, false, false, pitch, gain);
			ss.play();
			addSoundSource(ss);
			return ss;
		}
		else
			return null;
	}

	@Override
	public void stopAnySound(String sfx)
	{
		Iterator<SoundSourceAL> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			SoundSourceAL soundSource = i.next();
			if(soundSource.soundData.getName().indexOf(sfx) != -1)
			{
				soundSource.destroy();
				i.remove();
			}
		}
	}

	@Override
	public void stopAnySound()
	{
		for (SoundSource ss : playingSoundSources)
			ss.destroy();
		playingSoundSources.clear();
	}

	int removeUnusedSources()
	{
		int j = 0;
		Iterator<SoundSourceAL> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			SoundSourceAL soundSource = i.next();
			if (soundSource.isDonePlaying())
			{
				//System.out.println("Removed sound source " + soundSource + " #" + soundSource.internalID + " for being inactive.");
				soundSource.destroy();
				i.remove();
				j++;
			}
		}
		return j;
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect)
	{
		return playSoundEffect(soundEffect, 0, 0, 0, 1, 1);
	}

	@Override
	public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient)
	{
		SoundData data = SoundsLibrary.obtainBufferedSample(musicName);
		if (data != null)
		{
			SoundSourceAL ss = new SoundSourceAL(data, x, y, z, false, ambient, pitch, gain);
			addSoundSource(ss);
			ss.play();
			return ss;
		}
		else
			return null;
	}

	@Override
	public int getMaxEffectsSlots()
	{
		return this.auxEffectsSlots.length;
	}

	@Override
	public boolean setEffectForSlot(int slot, SoundEffect effect)
	{
		if(auxEffectsSlots.length <= 0)
			return false;
		else if(slot >= 0 && slot < auxEffectsSlots.length)
		{
			auxEffectsSlots[slot] = effect;
			return true;
		}
		else
			return false;
	}
	
	public int getSlotForEffect(SoundEffect effect)
	{
		for(int i = 0; i < auxEffectsSlots.length; i++)
		{
			if(auxEffectsSlots[i].equals(effect))
				return i;
		}
		return -1;
	}
}
