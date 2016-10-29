package io.xol.chunkstories.server.propagation;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.sound.SoundEffect;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.sound.SoundSource;
import io.xol.chunkstories.net.packets.PacketSoundSource;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;
import io.xol.engine.sound.sources.SoundSourceVirtual;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VirtualServerSoundManager implements SoundManager
{
	WorldServer worldServer;
	Server server;

	//We weakly keep track of playing sounds, the server has no idea of the actual sound data and this does not keeps track of played sounds !
	Set<WeakReference<SoundSourceVirtual>> allPlayingSoundSources = ConcurrentHashMap.newKeySet();

	//Each player has his own instance of the soundManager
	Set<ServerPlayerVirtualSoundManager> playersSoundManagers = ConcurrentHashMap.newKeySet();

	public class ServerPlayerVirtualSoundManager implements SoundManager
	{

		ServerPlayer serverPlayer;

		//As above, individual players have their playing sound sources kept track of
		Set<WeakReference<SoundSourceVirtual>> playingSoundSources = ConcurrentHashMap.newKeySet();

		public ServerPlayerVirtualSoundManager(ServerPlayer serverPlayer)
		{
			this.serverPlayer = serverPlayer;
			playersSoundManagers.add(this);
		}

		void update()
		{
			//Stops caring when the player is disconnected
			if (!serverPlayer.isConnected())
			{
				playersSoundManagers.remove(this);
			}

			//TODO:
			//Player should only be aware of sound's existence if close enought, this will be ensured later
		}

		public void addSourceToPlayer(SoundSourceVirtual soundSource)
		{
			PacketSoundSource packet = new PacketSoundSource();
			packet.soundSourceToSend = soundSource;
			serverPlayer.getPlayerConnection().pushPacket(packet);
			playingSoundSources.add(new WeakReference<SoundSourceVirtual>(soundSource));
		}

		@Override
		public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain)
		{
			SoundSourceVirtual soundSource = new SoundSourceVirtual(soundEffect, x, y, z, false, false, pitch, gain, false);
			//Play the sound effect for everyone
			addSourceToEveryone(soundSource, this);
			return soundSource;
		}

		@Override
		public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient)
		{
			SoundSourceVirtual soundSource = new SoundSourceVirtual(musicName, x, y, z, false, ambient, pitch, gain, true);
			//Play the sound effect for everyone
			addSourceToEveryone(soundSource, this);
			return soundSource;
		}

		@Override
		public SoundSource playSoundEffect(String soundEffect)
		{
			return null;
		}

		@Override
		public void stopAnySound(String soundEffect)
		{
			Iterator<SoundSource> i = getAllPlayingSounds();
			while (i.hasNext())
			{
				SoundSource s = i.next();
				if (s.getSoundName().indexOf(soundEffect) != -1)
				{
					s.stop();
				}
			}
		}

		@Override
		public void stopAnySound()
		{
			Iterator<SoundSource> i = getAllPlayingSounds();
			while (i.hasNext())
			{
				i.next().stop();
			}
		}

		/**
		 * Alls sounds we kept track of playing for this player
		 */
		@Override
		public Iterator<SoundSource> getAllPlayingSounds()
		{
			return new Iterator<SoundSource>()
			{
				Iterator<WeakReference<SoundSourceVirtual>> iterator = playingSoundSources.iterator();
				SoundSource next = null;

				@Override
				public boolean hasNext()
				{
					if (next != null)
						return true;

					while (iterator.hasNext() && next == null)
					{
						WeakReference<SoundSourceVirtual> weakReference = iterator.next();
						SoundSourceVirtual soundSource = weakReference.get();
						if (soundSource == null || soundSource.isDonePlaying())
						{
							iterator.remove();
							continue;
						}

						next = soundSource;
					}

					return false;
				}

				@Override
				public SoundSource next()
				{
					if (next == null)
						hasNext();

					SoundSource oldNext = next;
					next = null;
					return oldNext;
				}

			};
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
			// TODO Auto-generated method stub

		}
	}

	public VirtualServerSoundManager(WorldServer worldServer, Server server)
	{
		this.worldServer = worldServer;
		this.server = server;
	}

	public void update()
	{
		//System.out.println("update srv sounds");
		Iterator<ServerPlayerVirtualSoundManager> i = playersSoundManagers.iterator();
		while (i.hasNext())
		{
			ServerPlayerVirtualSoundManager playerSoundManager = i.next();
			playerSoundManager.update();
		}
	}

	private void addSourceToEveryone(SoundSourceVirtual soundSource, ServerPlayerVirtualSoundManager exceptHim)
	{
		Iterator<ServerPlayerVirtualSoundManager> i = playersSoundManagers.iterator();
		while (i.hasNext())
		{
			ServerPlayerVirtualSoundManager playerSoundManager = i.next();
			if (!playerSoundManager.equals(exceptHim))
				playerSoundManager.addSourceToPlayer(soundSource);
		}

		allPlayingSoundSources.add(new WeakReference<SoundSourceVirtual>(soundSource));
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect, float x, float y, float z, float pitch, float gain)
	{
		SoundSourceVirtual soundSource = new SoundSourceVirtual(soundEffect, x, y, z, false, false, pitch, gain, false);
		//Play the sound effect for everyone
		addSourceToEveryone(soundSource, null);
		return soundSource;
	}

	@Override
	public SoundSource playSoundEffect(String soundEffect)
	{
		// TODO Have yet to specify on playing GUI sounds for everyone 
		return null;
	}

	@Override
	public SoundSource playMusic(String musicName, float x, float y, float z, float pitch, float gain, boolean ambient)
	{
		SoundSourceVirtual soundSource = new SoundSourceVirtual(musicName, x, y, z, false, ambient, pitch, gain, true);
		//Play the sound effect for everyone
		addSourceToEveryone(soundSource, null);
		return soundSource;
	}

	@Override
	public void stopAnySound(String soundEffect)
	{
		Iterator<SoundSource> i = getAllPlayingSounds();
		while (i.hasNext())
		{
			SoundSource s = i.next();
			if (s.getSoundName().indexOf(soundEffect) != -1)
			{
				s.stop();
			}
		}
	}

	@Override
	public void stopAnySound()
	{
		Iterator<SoundSource> i = getAllPlayingSounds();
		while (i.hasNext())
		{
			i.next().stop();
		}
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

	@Override
	public Iterator<SoundSource> getAllPlayingSounds()
	{
		return new Iterator<SoundSource>()
		{
			Iterator<WeakReference<SoundSourceVirtual>> iterator = allPlayingSoundSources.iterator();
			SoundSource next = null;

			@Override
			public boolean hasNext()
			{
				if (next != null)
					return true;

				while (iterator.hasNext() && next == null)
				{
					WeakReference<SoundSourceVirtual> weakReference = iterator.next();
					SoundSourceVirtual soundSource = weakReference.get();
					if (soundSource == null || soundSource.isDonePlaying())
					{
						iterator.remove();
						continue;
					}

					next = soundSource;
				}

				return false;
			}

			@Override
			public SoundSource next()
			{
				if (next == null)
					hasNext();

				SoundSource oldNext = next;
				next = null;
				return oldNext;
			}

		};
	}

}
