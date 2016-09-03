package io.xol.chunkstories.content.sandbox;

import java.util.Iterator;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.PluginsManager;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldNetworked;
import io.xol.chunkstories.world.WorldServer;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Sandboxed thread that runs all the game logic for one world, thus including foreign code
 */
public class GameLogicThread extends Thread implements GameLogic
{
	private WorldImplementation world;

	boolean die = false;

	public GameLogicThread(WorldImplementation world, SecurityManager securityManager)
	{
		this.world = world;
		this.world.setLogicThread(this);
		
		this.setName("world " + world.getWorldInfo().getInternalName()+" logic thread");
		this.setPriority(Constants.MAIN_SINGLEPLAYER_LOGIC_THREAD_PRIORITY);
		
		this.start();
	}

	public void run()
	{
		//Installs a custom SecurityManager
		System.out.println(System.getSecurityManager());
		//System.setSecurityManager(securityManager);

		while (!die)
		{
			//Dirty performance metric :]
			//perfMetric();
			
			//Timings
			fps = 1f / ((System.nanoTime() - lastTimeNs) / 1000f / 1000f / 1000f);
			lastTimeNs = System.nanoTime();
			
			//Updates controller/s views
			if(world instanceof WorldClient)
				Client.getInstance().getClientSideController().updateUsedWorldBits();
			if(world instanceof WorldServer)
			{
				Iterator<Player> i = ((WorldServer)world).getPlayers();
				while(i.hasNext())
				{
					Player p = i.next();
					p.updateUsedWorldBits();
				}
			}
			
			//Processes incomming pending packets in synch with game logic
			if(world instanceof WorldNetworked)
				((WorldNetworked) world).processIncommingPackets();
			
			//Tick the world ( mostly entities )
			world.tick();
			
			//Every second, unloads unused stuff
			if(world.getTicksElapsed() % 60 == 0)
			{
				//Compresses pending chunk summaries
				Iterator<RegionImplementation> loadedChunksHolders = world.getRegionsHolder().getLoadedRegions();
				while(loadedChunksHolders.hasNext())
				{
					RegionImplementation region = loadedChunksHolders.next();
					region.compressChangedChunks();
				}
				
				//Delete unused world data
				world.unloadUselessData();
			}
			
			//Game logic is 60 ticks/s
			sync(getTargetFps());
		}
	}

	float fps = 0.0f;
	
	long lastTime = 0;
	long lastTimeNs = 0;

	@Override
	public int getTargetFps()
	{
		return 60;
	}

	public double getSimulationFps()
	{
		return (double) (Math.floor(fps * 100f) / 100f);
	}
	
	public double getSimulationSpeed()
	{
		return 1.0;
	}
	
	public void perfMetric()
	{
		double ms = Math.floor((System.nanoTime() - lastTimeNs) / 10000.0) / 100.0;
		String kek = "";
		double d = 0.02;
		for(double i = 0; i < 1.0; i+= d)
			kek += Math.abs(ms - 16 - i) > d ? " " : "|";
		System.out.println(kek + ms + "ms");
	}
	
	public void sync(int fps)
	{
		if (fps <= 0)
			return;

		long errorMargin = 1000 * 1000; // 1 millisecond error margin for
										// Thread.sleep()
		long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame

		// if smaller than sleepTime burn for errorMargin + remainder micro &
		// nano seconds
		long burnTime = Math.min(sleepTime, errorMargin + sleepTime % (1000 * 1000));

		long overSleep = 0; // time the sleep or burn goes over by

		try
		{
			while (true)
			{
				long t = System.nanoTime() - lastTime;

				if (t < sleepTime - burnTime)
				{
					Thread.sleep(1);
				}
				else if (t < sleepTime)
				{
					// burn the last few CPU cycles to ensure accuracy
					Thread.yield();
				}
				else
				{
					overSleep = Math.min(t - sleepTime, errorMargin);
					break; // exit while loop
				}
			}
		}
		catch (InterruptedException e)
		{
		}

		lastTime = System.nanoTime() - overSleep;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.content.sandbox.GameLogic#getPluginsManager()
	 */
	@Override
	public PluginsManager getPluginsManager()
	{
		if(world instanceof WorldClient)
			return Client.getInstance().getPluginsManager();
		else if(world instanceof WorldServer)
			return Server.getInstance().getPluginsManager();
		return null;
	}
	
	public void stopLogicThread()
	{
		die = true;
	}
}
