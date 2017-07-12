package io.xol.chunkstories.tools;

import java.util.Iterator;

import io.xol.chunkstories.api.GameContext;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfoFile;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.io.IOTasksImmediate;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTool extends WorldImplementation implements WorldMaster
{
	private final GameContext toolContext;
	
	public WorldTool(GameContext toolContext, WorldInfoFile info) {
		this(toolContext, info, true);
	}
	
	public WorldTool(GameContext toolContext, WorldInfoFile info, boolean immediateIO)
	{
		super(toolContext, info);//new WorldInfoImplementation(new File(worldDir + "/info.world"), new File(worldDir).getName()));

		this.toolContext = toolContext;
		
		if(immediateIO)
			ioHandler = new IOTasksImmediate(this);
		else {
			//Normal IO.
			ioHandler = new IOTasks(this);
			ioHandler.start();
		}
		//ioHandler.start();
	}

	/*public WorldTool(GameContext toolContext, File csWorldDir)
	{
		this(toolContext, csWorldDir.getAbsolutePath());
	}*/
	
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
		public void setListenerPosition(float x, float y, float z, Vector3fc lookAt, Vector3fc up)
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
		public void spawnParticleAtPosition(String particleTypeName, Vector3dc location)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void spawnParticleAtPositionWithVelocity(String particleTypeName, Vector3dc location, Vector3dc velocity)
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
		public void drawDecal(Vector3dc position, Vector3dc orientation, Vector3dc size, String decalName)
		{
			// TODO Auto-generated method stub
			
		}
		
	}

	@Override
	public void spawnPlayer(Player player)
	{
		throw new UnsupportedOperationException("spawnPlayer");
	}

	@Override
	public IterableIterator<Player> getPlayers()
	{
		throw new UnsupportedOperationException("getPlayers");
	}

	@Override
	public Player getPlayerByName(String playerName)
	{
		throw new UnsupportedOperationException("getPlayers");
	}
}
