//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.concurrency.UniqueQueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;



/**
 * This thread does I/O work in queue. Extended by IOTaskMultiplayerClient and IOTaskMultiplayerServer for the client/server model.
 */
public class IOTasks extends Thread implements TaskExecutor
{
	protected WorldImplementation world;

	protected UniqueQueue<IOTask> tasks = new UniqueQueue<IOTask>();
	protected Semaphore tasksCounter = new Semaphore(0);

	protected LZ4Factory factory = LZ4Factory.fastestInstance();
	protected LZ4FastDecompressor decompressor = factory.fastDecompressor();

	protected int worldSizeInChunks = 0;
	protected int worldHeightInChunks = 0;

	private IOTask DIE = new IOTask() {

		@Override
		protected boolean task(TaskExecutor taskExecutor) {
			return false;
		}
	};
	
	//Per-thread buffer
	protected static ThreadLocal<byte[]> unCompressedDataBuffer = new ThreadLocal<byte[]>()
	{
		@Override
		protected byte[] initialValue()
		{
			//One-chunk buffer
			return new byte[32 * 32 * 32 * 4];
		}
	};

	public IOTasks(WorldImplementation world)
	{
		this.world = world;
		worldSizeInChunks = world.getSizeInChunks();
		worldHeightInChunks = world.getMaxHeight() / 32;
	}

	public boolean scheduleTask(IOTask task)
	{
		boolean code = tasks.add(task);
		if(code) {
			tasksCounter.release();
		}
		return code;
	}

	@Override
	public String toString()
	{
		return "[IO :" + getSize() + " in queue.]";
	}

	@Override
	public void run()
	{
		logger().info("IO Thread started for '"+this.world.getWorldInfo().getName()+"'");

		this.setPriority(Constants.IO_THREAD_PRIOTITY);
		this.setName("IO thread for '"+this.world.getWorldInfo().getName()+"'");
		while (true)
		{
			IOTask task = null;

			tasksCounter.acquireUninterruptibly();
			
			task = tasks.poll();
			if (task == null)
			{
				//Crash and burn
				System.exit(-1);
				
				/*System.out.println("out of work, sleeping");
				try
				{
					synchronized (this)
					{
						wait();
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}*/
			}
			else if(task == DIE) {
				break;
			}
			else
			{
				// Run it
				try
				{
					//ChunkStoriesLogger.getInstance().info("processing task : "+task);
					boolean taskSuccessfull = task.run(this);
					// If it returns false, requeue it.
					
					//if (!taskSuccessfull)
					//	tasks.add(task);
					if(taskSuccessfull == false)
						rescheduleTask(task);
					//else
					//	tasksQueueSize.decrementAndGet();
					
					
					// Runs a post-run operation
					//else if (task.postRunOperation != null)
					//	task.postRunOperation.run();

				}
				catch (Exception e)
				{
					logger().warn("Exception occured when processing task : " + task);
					e.printStackTrace();
				}
			}
		}
		System.out.println("IOTasks worker thread stopped");
	}

	void rescheduleTask(IOTask task)
	{
		tasks.add(task);
		tasksCounter.release();
		
		//tasksRescheduled++;
	}
	
	public int getSize()
	{
		int i = 0;
		synchronized (tasks)
		{
			i = tasks.size();
		}
		return i;
	}

	public abstract class IOTask extends Task
	{
		@Override
		public void cancel()
		{
			super.cancel();
		}
	}

	/**
	 * Loads the content of a region chunk slot
	 */
	public class IOTaskLoadChunk extends IOTask
	{
		ChunkHolderImplementation chunkSlot;

		public IOTaskLoadChunk(ChunkHolderImplementation chunkSlot)
		{
			this.chunkSlot = chunkSlot;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			// When a loader was removed from the world, remaining operations on it are discarded
			if (chunkSlot.getRegion().isUnloaded())
				return true;
			
			// And so are redudant operations
			if (chunkSlot.isChunkLoaded())
				return true;

			Region region = chunkSlot.getRegion();
			Region actualRegion = world.getRegion(region.getRegionX(), region.getRegionY(), region.getRegionZ());
			
			if(region != actualRegion) {
				System.out.println("Some quircky race condition led to this region being discarded then loaded again but without raising the isUnloaded() flag !");
				System.out.println(region + " vs: " + actualRegion);
				return true;
			}
			
			// If for some reasons the chunks holder's are still not loaded, we requeue the job
			if (!chunkSlot.getRegion().isDiskDataLoaded())
				return false;

			CompressedData compressedData = chunkSlot.getCompressedData();
			//Not yet generated chunk; call the generator
			if (compressedData == null) {
				Chunk chunk = chunkSlot.createChunk();
				world.getGenerator().generateChunk(chunk);
			}
			//Normal voxel data is present, uncompressed it then load it to the chunk
			else {
				CubicChunk chunk = chunkSlot.createChunk(compressedData);
				
				//TODO Look into this properly
				//We never want to mess with that when we are the world
				if(!(world instanceof WorldTool)) {
					//chunkSlot.getChunk().computeVoxelLightning(false);
					chunk.lightBaker().requestLightningUpdate();
				}
			}

			//chunkSlot.setChunk(result);
			synchronized (chunkSlot)
			{
				chunkSlot.notifyAll();
			}
			return true;
		}

		@Override
		public String toString()
		{
			return "[IOTaskLoadChunk " + chunkSlot + "]";
		}

		@Override
		public boolean equals(Object o)
		{
			if (o != null && o instanceof IOTaskLoadChunk)
			{
				IOTaskLoadChunk comp = ((IOTaskLoadChunk) o);

				//If not the same region, don't even bother
				if (!comp.chunkSlot.getRegion().equals(this.chunkSlot.getRegion()))
					return false;

				//Complete match of coordinates ?
				if (comp.chunkSlot.getInRegionX() == this.chunkSlot.getInRegionX() && comp.chunkSlot.getInRegionY() == this.chunkSlot.getInRegionY() && comp.chunkSlot.getInRegionZ() == this.chunkSlot.getInRegionZ())
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return (int) ((65536 + 65536L * chunkSlot.getInRegionX() + 256 * chunkSlot.getInRegionY() + chunkSlot.getInRegionZ()) % 2147483647);
		}

	}

	public IOTask requestChunkLoad(ChunkHolderImplementation chunkSlot)
	{
		IOTaskLoadChunk task = new IOTaskLoadChunk(chunkSlot);
		if (scheduleTask(task))
			return task;
		return null;
	}

	public class IOTaskLoadRegion extends IOTask
	{
		RegionImplementation region;

		public IOTaskLoadRegion(RegionImplementation holder)
		{
			this.region = holder;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			//Check no saving operations are occuring
			IOTaskSaveRegion saveRegionTask = new IOTaskSaveRegion(region);
			if (tasks != null && tasks.contains(saveRegionTask))
			{
				//System.out.println("A save operation is still running on " + holder + ", waiting for it to complete.");
				return false;
			}

			if (region.handler.exists())
			{
				try
				{
					FileInputStream fist = new FileInputStream(region.handler.file);
					DataInputStream in = new DataInputStream(fist);
					
					region.handler.load(in);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
					return true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return true;
				}
			}
			//Else if no file exists
			else
			{
				/*RegionSummaryImplementation regionSummary = world.getRegionsSummariesHolder().getRegionSummaryWorldCoordinates(region.regionX * 256, region.regionZ * 256);
				//Require a chunk summary to be generated first !
				if (regionSummary == null || !regionSummary.isLoaded())
				{
					return false;
				}*/
				//Generate this crap !
				//region.generateAll();
				//Pre bake phase 1 lightning
			}

			//Marking the holder as loaded allows the game to remove it and unload it, so we set the timer to have a time frame until it naturally unloads.
			region.resetUnloadCooldown();
			region.setDiskDataLoaded(true);

			//world.unloadsUselessData();
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o != null && o instanceof IOTaskLoadRegion)
			{
				IOTaskLoadRegion comp = ((IOTaskLoadRegion) o);
				if (comp.region.regionX == region.regionX && comp.region.regionY == this.region.regionY && comp.region.regionZ == this.region.regionZ)
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return (874 + 64 * region.regionX + 22 * region.regionY + 999 * region.regionZ) % 2147483647;
		}
	}

	public boolean isDoneSavingRegion(RegionImplementation holder)
	{
		if (!(this.world instanceof WorldMaster))
			return true;

		//Check no saving operations are occuring
		IOTaskSaveRegion saveRegionTask = new IOTaskSaveRegion(holder);
		if (tasks != null && tasks.contains(saveRegionTask))
			return false;
		return true;
	}

	public void requestRegionLoad(RegionImplementation holder)
	{
		if (!isDoneSavingRegion(holder))
			return;

		IOTask task = new IOTaskLoadRegion(holder);
		scheduleTask(task);
	}

	public class IOTaskSaveRegion extends IOTask
	{
		RegionImplementation region;

		public IOTaskSaveRegion(RegionImplementation holder)
		{
			this.region = holder;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			region.handler.savingOperations.incrementAndGet();
			
			// First compress all loaded chunks !
			region.compressAll();
			
			try
			{
				//Create the necessary directory structure if needed
				region.handler.file.getParentFile().mkdirs();
				
				//Create the output stream
				FileOutputStream outputFileStream = new FileOutputStream(region.handler.file);
				DataOutputStream dos = new DataOutputStream(outputFileStream);
				
				region.handler.save(dos);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			//Let go
			this.region.handler.savingOperations.decrementAndGet();
			
			synchronized (region.handler)
			{
				region.handler.notifyAll();
			}
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o != null && o instanceof IOTaskSaveRegion)
			{
				IOTaskSaveRegion comp = ((IOTaskSaveRegion) o);
				if (comp.region.regionX == region.regionX && comp.region.regionY == this.region.regionY && comp.region.regionZ == this.region.regionZ)
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return (666778 + 64 * region.regionX + 22 * region.regionY + 999 * region.regionZ) % 2147483647;
		}
	}

	public IOTask requestRegionSave(RegionImplementation holder)
	{
		if (!holder.isDiskDataLoaded())
			return null;

		IOTask task = new IOTaskSaveRegion(holder);
		if(!scheduleTask(task)) {
			//I really thinks this is smart
			
			//System.out.println("Could not request a region save, another one is still going");
			//System.out.println("Creating a task to submit it once we can");
			scheduleTask(new IOTask() {

				@Override
				protected boolean task(TaskExecutor taskExecutor) {
					return scheduleTask(task);
				}
				
			});
		}
		
		return task;
	}

	//Irrelevant. We unload first actually
	/*@Deprecated
	private void requestRegionSaveAndUnload(RegionImplementation holder)
	{
		if (!holder.isDiskDataLoaded())
			return;

		IOTask task = new IOTaskSaveRegion(holder) {

			@Override
			public boolean task(TaskExecutor taskExecutor) {
				boolean worked = super.task(taskExecutor);
				if(worked) {
					holder.unload();
				}
				
				return worked;
			}
			
		};

		scheduleTask(task);
	}*/

	public class IOTaskLoadSummary extends IOTask
	{
		RegionSummaryImplementation summary;

		public IOTaskLoadSummary(RegionSummaryImplementation summary)
		{
			this.summary = summary;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			if (summary.isLoaded())
				return true;
			if (summary.handler.exists())
			{
				try
				{
					FileInputStream in = new FileInputStream(summary.handler);

					byte[] size = new byte[4];

					int[] heights = new int[256*256];
					int[] ids = new int[256*256];

					try
					{
						in.read(size);
						int s = ByteBuffer.wrap(size).asIntBuffer().get(0);
						byte[] compressed = new byte[s];
						in.read(compressed);

						byte[] decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
						IntBuffer ib = ByteBuffer.wrap(decompressed).asIntBuffer();
						for (int i = 0; i < 256 * 256; i++)
							heights[i] = ib.get();

						in.read(size);
						s = ByteBuffer.wrap(size).asIntBuffer().get(0);
						compressed = new byte[s];
						in.read(compressed);

						decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
						ib = ByteBuffer.wrap(decompressed).asIntBuffer();
						for (int i = 0; i < 256 * 256; i++)
							ids[i] = ib.get();

						in.close();
					}
					catch (Exception e)
					{
						logger().error("Could not load load chunk summary at " + summary + " cause: " + e.getMessage());
					}

					summary.setSummaryData(heights, ids);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				// System.out.println("Couldn't find file : " + summary.handler.getAbsolutePath());
				// Generate summary according to generator heightmap
				int h, t;
				
				int[] heights = new int[256*256];
				int[] ids = new int[256*256];
				
				for (int x = 0; x < 256; x++)
					for (int z = 0; z < 256; z++)
					{
						h = world.getGenerator().getHeightAt(x + summary.getRegionX() * 256, z + summary.getRegionZ() * 256);
						t = world.getGenerator().getTopDataAt(x + summary.getRegionX() * 256, z + summary.getRegionZ() * 256);
						heights[x * 256 + z] = h;
						ids[x * 256 + z] = t;
					}

				summary.setSummaryData(heights, ids);
				// then save

				//summary.save(summary.handler);

				// Thread.dumpStack();
			}
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof IOTaskLoadSummary)
			{
				IOTaskLoadSummary comp = ((IOTaskLoadSummary) o);
				if (comp.summary.getRegionX() == this.summary.getRegionX() && comp.summary.getRegionZ() == this.summary.getRegionZ())
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return 1111 + summary.getRegionX() + summary.getRegionZ() * 256;
		}
	}

	public Fence requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		IOTaskLoadSummary task = new IOTaskLoadSummary(summary);
		scheduleTask(task);
		
		return task;
	}

	public class IOTaskSaveSummary extends IOTask
	{
		RegionSummaryImplementation summary;

		public IOTaskSaveSummary(RegionSummaryImplementation summary)
		{
			this.summary = summary;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			try
			{
				if(!summary.isLoaded())
					return true;
				
				summary.handler.getParentFile().mkdirs();
				if (!summary.handler.exists())
					summary.handler.createNewFile();
				FileOutputStream out = new FileOutputStream(summary.handler);

				int[] heights = summary.getHeightData();
				int[] ids = summary.getVoxelData();
				
				ByteBuffer writeMe = ByteBuffer.allocate(256 * 256 * 4);

				for (int i = 0; i < 256 * 256; i++)
					writeMe.putInt(heights[i]);

				byte[] compressed = RegionSummaryImplementation.compressor.compress(writeMe.array());

				int compressedSize = compressed.length;

				byte[] size = ByteBuffer.allocate(4).putInt(compressedSize).array();
				out.write(size);
				out.write(compressed);

				writeMe.clear();
				for (int i = 0; i < 256 * 256; i++)
					writeMe.putInt(ids[i]);

				compressed = RegionSummaryImplementation.compressor.compress(writeMe.array());
				compressedSize = compressed.length;

				size = ByteBuffer.allocate(4).putInt(compressedSize).array();
				out.write(size);
				out.write(compressed);

				out.close();
				// System.out.println("saved");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}

		/*
		 * This code was utter bullshit. You know it, I know it.
		
		@Override
		public boolean equals(Object o)
		{
			if(o instanceof IOTaskLoadSummary)
			{
				IOTaskLoadSummary comp = ((IOTaskLoadSummary)o);
				if(comp.summary.getRegionX() == this.summary.getRegionX() && comp.summary.getRegionZ() == this.summary.getRegionZ())
					return true;
			}
			//All saves request are unique
			//return false;
		}*/

		@Override
		public int hashCode()
		{
			return 7777 + summary.getRegionX() + summary.getRegionZ() * 256;
		}
	}

	public IOTaskSaveSummary requestRegionSummarySave(RegionSummaryImplementation summary)
	{
		IOTaskSaveSummary task = new IOTaskSaveSummary(summary);
		scheduleTask(task);
		
		return task;
	}

	public void notifyChunkUnload(int chunkX, int chunkY, int chunkZ)
	{

	}

	public void kill()
	{
		scheduleTask(DIE);
		synchronized (this)
		{
			notifyAll();
		}
	}

	public void dumpIOTaks()
	{
		System.out.println("dumping io tasks");
		
		//Hardcoding a security because you can fill the queue faster than you can iterate it
		int hardLimit = 500;
		Iterator<IOTask> i = this.tasks.iterator();
		while (i.hasNext())
		{
			IOTask task = i.next();
			hardLimit--;
			if(hardLimit < 0)
				return;
			System.out.println(task);
		}
	}

	public void waitThenKill()
	{
		synchronized (this)
		{
			notifyAll();
		}
		
		//Wait for it to finish what it's doing
		while (this.tasks.size() > 0)
		{
			try
			{
				sleep(150L);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		scheduleTask(DIE);
	}
	
	private static final Logger logger = LoggerFactory.getLogger("world.io");
	public Logger logger() {
		return logger;
	}
}
