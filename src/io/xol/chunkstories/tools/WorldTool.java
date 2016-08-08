package io.xol.chunkstories.tools;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksImmediate;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTool extends WorldImplementation implements WorldMaster
{
	public WorldTool(String worldDir)
	{
		super(new WorldInfo(new File(worldDir + "/info.txt"), new File(worldDir).getName()));

		ioHandler = new IOTasksImmediate(this);
		//ioHandler.start();
	}

	public WorldTool(File csWorldDir)
	{
		this(csWorldDir.getAbsolutePath());
	}

	@Override
	public void trimRemovableChunks()
	{

	}

	@Override
	public SoundManager getSoundManager()
	{
		return nullSoundManager;
	}

	NullSoundManager nullSoundManager = new NullSoundManager();

	class NullSoundManager implements SoundManager
	{

		@Override
		public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain)
		{

			return null;
		}

		@Override
		public SoundSource playSoundEffect(String soundEffect)
		{

			return null;
		}

		@Override
		public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient)
		{

			return null;
		}

		@Override
		public void stopAnySound(String soundEffect)
		{

		}

		@Override
		public void stopAnySound()
		{
		}

		@Override
		public Iterator<SoundSource> getAllPlayingSounds()
		{
			return null;
		}

		@Override
		public int getMaxEffectsSlots()
		{
			return 0;
		}

		@Override
		public boolean setEffectForSlot(int slot, SoundEffect effect)
		{
			return false;
		}

		@Override
		public void setListenerPosition(float x, float y, float z, FloatBuffer rot)
		{
		}

	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return nullParticlesManager;
	}

	NullParticlesManager nullParticlesManager = new NullParticlesManager();

	class NullParticlesManager implements ParticlesManager
	{

		@Override
		public void spawnParticleAtPosition(String particleTypeName, Vector3d location)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3d location, Vector3d velocity)
		{
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return nullDecalsManager;
	}
	
	NullDecalsManager nullDecalsManager = new NullDecalsManager();
	
	class NullDecalsManager implements DecalsManager 
	{

		@Override
		public void drawDecal(Vector3d position, Vector3d orientation, Vector3d size, String decalName)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
}
