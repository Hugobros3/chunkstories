package io.xol.chunkstories.tools;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Iterator;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.io.IOTasksImmediate;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTool extends WorldImplementation implements WorldMaster
{
	private final GameContext toolContext;
	
	public WorldTool(GameContext toolContext, String worldDir)
	{
		super(toolContext, new WorldInfo(new File(worldDir + "/info.txt"), new File(worldDir).getName()));

		this.toolContext = toolContext;
		
		ioHandler = new IOTasksImmediate(this);
		//ioHandler.start();
	}

	public WorldTool(GameContext toolContext, File csWorldDir)
	{
		this(toolContext, csWorldDir.getAbsolutePath());
	}
	
	public GameContext getGameContext()
	{
		return toolContext;
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
		public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain, float attStart, float attEnd)
		{

			return null;
		}

		@Override
		public SoundSource playSoundEffect(String soundEffect)
		{

			return null;
		}

		@Override
		public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient, float attStart, float attEnd)
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
		public void spawnParticleAtPosition(String particleTypeName, Vector3dm location)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dm location, Vector3dm velocity)
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
		public void drawDecal(Vector3dm position, Vector3dm orientation, Vector3dm size, String decalName)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
}
