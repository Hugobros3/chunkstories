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

import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;
import io.xol.engine.sound.ogg.SoundDataOggSample;
import io.xol.engine.sound.sources.ALBufferedSoundSource;
import io.xol.engine.sound.sources.ALSoundSource;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ALSoundManager implements SoundManager
{
	protected Queue<ALSoundSource> playingSoundSources = new ConcurrentLinkedQueue<ALSoundSource>();
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
			ChunkStoriesLogger.getInstance().info("OpenAL context successfully created, version = " + alVersion);
			ChunkStoriesLogger.getInstance().info("OpenAL Extensions avaible : " + alExtensions);
			efxOn = EFXUtil.isEfxSupported();
			ChunkStoriesLogger.getInstance().info("EFX extension support : " + (efxOn ? "yes" : "no"));
			if (efxOn)
			{
				//Reset error
				alGetError();
				List<Integer> auxSlotsIds = new ArrayList<Integer>();
				while (true)
				{
					int generated_id = alGenAuxiliaryEffectSlots();
					int error = alGetError();
					if (error != AL_NO_ERROR)
						break;
					auxSlotsIds.add(generated_id);
				}
				auxEffectsSlotsId = new int[auxSlotsIds.size()];
				int j = 0;
				for (int i : auxSlotsIds)
				{
					auxEffectsSlotsId[j] = i;
					j++;
				}
				auxEffectsSlots = new SoundEffect[auxSlotsIds.size()];
				ChunkStoriesLogger.getInstance().info(auxEffectsSlots.length + " avaible auxiliary effects slots.");
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
		catch (LWJGLException e)
		{
			System.out.println("Failed to start sound system !");
			e.printStackTrace();
		}
	}

	public void destroy()
	{
		for (SoundSource ss : playingSoundSources)
			ss.stop();
		AL.destroy();
	}

	public void update()
	{
		int result;
		if ((result = alGetError()) != AL_NO_ERROR)
			System.out.println("error at iter :" + SoundDataOggSample.getALErrorString(result));
		removeUnplayingSources();
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			ALSoundSource soundSource = i.next();
			soundSource.update(this);
		}
	}

	public float x, y, z;

	@Override
	public void setListenerPosition(float x, float y, float z, Vector3fm lookAt, Vector3fm up)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		FloatBuffer posScratch = BufferUtils.createFloatBuffer(3).put(new float[] { x, y, z });
		posScratch.flip();
		alListener(AL_POSITION, posScratch);
		//AL10.alListener(AL10.AL_VELOCITY, xxx);
		

		FloatBuffer rotScratch = BufferUtils.createFloatBuffer(6).put(new float[] { lookAt.getX(), lookAt.getY(), lookAt.getZ(), up.getX(), up.getY(), up.getZ() });
		rotScratch.flip();
		alListener(AL_ORIENTATION, rotScratch);
		//FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });
	}

	//long countTo9223372036854775808 = 0L;

	public void addSoundSource(ALSoundSource soundSource)
	{
		soundSource.play();
		//countTo9223372036854775808++;
		//soundSource.soundSourceUUID = countTo9223372036854775808;
		playingSoundSources.add(soundSource);
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain, float attStart, float attEnd)
	{
		try
		{
			ALSoundSource ss = new ALSoundSource(soundEffect, x, y, z, false, false, pitch, gain, attStart, attEnd);
			addSoundSource(ss);
			return ss;
		}
		catch (SoundEffectNotFoundException e)
		{
			System.out.println("Sound not found "+soundEffect);
		}
		return new DummySound();
	}

	@Override
	public void stopAnySound(String sfx)
	{
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			ALSoundSource soundSource = i.next();
			if (soundSource.soundData.getName().indexOf(sfx) != -1)
			{
				soundSource.stop();
				i.remove();
			}
		}
	}

	@Override
	public void stopAnySound()
	{
		for (SoundSource ss : playingSoundSources)
			ss.stop();
		playingSoundSources.clear();
	}

	int removeUnplayingSources()
	{
		int j = 0;
		Iterator<ALSoundSource> i = playingSoundSources.iterator();
		while (i.hasNext())
		{
			SoundSource soundSource = i.next();
			if (soundSource.isDonePlaying())
			{
				soundSource.stop();
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
	public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient, float attStart, float attEnd)
	{
		try
		{
			ALSoundSource ss = new ALBufferedSoundSource(musicName, x, y, z, false, ambient, pitch, gain, attStart, attEnd);
			addSoundSource(ss);
			return ss;
		}
		catch (SoundEffectNotFoundException e)
		{
		}
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
		if (auxEffectsSlots.length <= 0)
			return false;
		else if (slot >= 0 && slot < auxEffectsSlots.length)
		{
			auxEffectsSlots[slot] = effect;
			return true;
		}
		else
			return false;
	}

	public int getSlotForEffect(SoundEffect effect)
	{
		for (int i = 0; i < auxEffectsSlots.length; i++)
		{
			if (auxEffectsSlots[i].equals(effect))
				return i;
		}
		return -1;
	}

	@Override
	public Iterator<SoundSource> getAllPlayingSounds()
	{
		return new Iterator<SoundSource>()
		{
			Iterator<ALSoundSource> i = playingSoundSources.iterator();

			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public SoundSource next()
			{
				return i.next();
			}
		};
	}
}
