//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.sound;

import static org.lwjgl.openal.AL10.AL_EXTENSIONS;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.AL_ORIENTATION;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_VERSION;
import static org.lwjgl.openal.AL10.alDistanceModel;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL10.alGetString;
import static org.lwjgl.openal.AL10.alListenerfv;
import static org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE_CLAMPED;
import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.ALC11.ALC_ALL_DEVICES_SPECIFIER;
import static org.lwjgl.openal.EXTEfx.alGenAuxiliaryEffectSlots;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALUtil;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.client.ClientSoundManager;
import io.xol.chunkstories.api.exceptions.SoundEffectNotFoundException;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.engine.sound.ogg.SoundDataOggSample;
import io.xol.engine.sound.sources.ALBufferedSoundSource;
import io.xol.engine.sound.sources.ALSoundSource;
import io.xol.engine.sound.sources.DummySoundSource;



public class ALSoundManager implements ClientSoundManager
{
	protected Queue<ALSoundSource> playingSoundSources = new ConcurrentLinkedQueue<ALSoundSource>();
	Random rng;

	Thread contextThread;
	// Are we allowed to use EFX effects
	public static boolean efxOn = false;
	
	private AtomicBoolean shutdownState = new AtomicBoolean(false);

	int[] auxEffectsSlotsId;
	
	private long device;
	private long context;
	
	public final static Logger logger = LoggerFactory.getLogger("sound");

	public ALSoundManager()
	{
		rng = new Random();
		try
		{
			device = alcOpenDevice((ByteBuffer)null);
	        if (device == MemoryUtil.NULL) {
	            throw new IllegalStateException("Failed to open the default device.");
	        }

	        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

	        logger.info("OpenALC10: " + deviceCaps.OpenALC10);
	        logger.info("OpenALC11: " + deviceCaps.OpenALC11);
	        logger.info("caps.ALC_EXT_EFX = " + deviceCaps.ALC_EXT_EFX);

	        if (deviceCaps.OpenALC11) {
	            List<String> devices = ALUtil.getStringList(MemoryUtil.NULL, ALC_ALL_DEVICES_SPECIFIER);
	            if (devices == null) {
	                //checkALCError(MemoryUtil.NULL);
	            } else {
	                for (int i = 0; i < devices.size(); i++) {
	                 	logger.debug(i + ": " + devices.get(i));
	                }
	            }
	        }

	        String defaultDeviceSpecifier = alcGetString(MemoryUtil.NULL, ALC_DEFAULT_DEVICE_SPECIFIER);
	        logger.info("Default device: " + defaultDeviceSpecifier);

	        context = alcCreateContext(device, (IntBuffer)null);
	        alcMakeContextCurrent(context);
	        //alcSetThreadContext(context);
	       
	        AL.createCapabilities(deviceCaps);

			
			alDistanceModel(AL_LINEAR_DISTANCE_CLAMPED);
			String alVersion = alGetString(AL_VERSION);
			String alExtensions = alGetString(AL_EXTENSIONS);
			contextThread = Thread.currentThread();
			logger().info("OpenAL context successfully created, version = " + alVersion);
			logger().info("OpenAL Extensions avaible : " + alExtensions);
			efxOn = false;//EFXUtil.isEfxSupported();
			logger().info("EFX extension support : " + (efxOn ? "yes" : "no"));
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
				//auxEffectsSlots = new SoundEffect[auxSlotsIds.size()];
				//logger().info(auxEffectsSlots.length + " avaible auxiliary effects slots.");
			}

			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
		            shutdown();
				}
			});
		}
		catch (Exception e)
		{
			logger.error("Failed to start sound system !");
			e.printStackTrace();
		}
	}

	private Logger logger() {
		return logger;
	}

	public void destroy()
	{
		for (SoundSource ss : playingSoundSources)
			ss.stop();

		shutdown();
	}
	
	private void shutdown() {
		if(shutdownState.compareAndSet(false, true)) {
	        alcDestroyContext(context);
	        alcCloseDevice(device);
			logger.info("OpenAL properly shut down.");
		}
	}

	public void update()
	{
		int result;
		if ((result = alGetError()) != AL_NO_ERROR)
			logger.error("error at iter :" + SoundDataOggSample.getALErrorString(result));
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
	public void setListenerPosition(float x, float y, float z, Vector3fc lookAt, Vector3fc up)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		FloatBuffer posScratch = MemoryUtil.memAllocFloat(3).put(new float[] { x, y, z });
		posScratch.flip();
		alListenerfv(AL_POSITION, posScratch);
		//AL10.alListener(AL10.AL_VELOCITY, xxx);
		

		FloatBuffer rotScratch = MemoryUtil.memAllocFloat(6).put(new float[] { lookAt.x(), lookAt.y(), lookAt.z(), up.x(), up.y(), up.z() });
		rotScratch.flip();
		alListenerfv(AL_ORIENTATION, rotScratch);
		//FloatBuffer listenerOri = BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });
	}

	public void addSoundSource(ALSoundSource soundSource)
	{
		soundSource.play();
		playingSoundSources.add(soundSource);
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect, Mode mode, Vector3dc position, float pitch, float gain, float attStart, float attEnd)
	{
		try
		{
			ALSoundSource ss;
			if(mode == Mode.STREAMED)
				ss = new ALBufferedSoundSource(soundEffect, position, pitch, gain, attStart, attEnd);
			else
				ss = new ALSoundSource(soundEffect, mode, position, pitch, gain, attStart, attEnd);
			
			addSoundSource(ss);
			return ss;
		}
		catch (SoundEffectNotFoundException e)
		{
			logger.warn("Sound not found "+soundEffect);
		}
		return new DummySoundSource();
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

	@Override
	public SoundSource replicateServerSoundSource(String soundName, Mode mode, Vector3dc position, float pitch, float gain, float attenuationStart, float attenuationEnd, long UUID) {
		try {
			ALSoundSource soundSource = null;
				
			if (mode == Mode.STREAMED)
				soundSource = new ALBufferedSoundSource(soundName, position, pitch, gain, attenuationStart, attenuationEnd);
			else
				soundSource = new ALSoundSource(soundName, mode, position, pitch, gain, attenuationStart, attenuationEnd);
			
			//Match the UUIDs
			soundSource.setUUID(UUID);
			addSoundSource(soundSource);
			
			return soundSource;
		}
		catch (SoundEffectNotFoundException e)
		{
			logger.warn("Sound not found "+soundName);
			return null;
		}
	}
}
