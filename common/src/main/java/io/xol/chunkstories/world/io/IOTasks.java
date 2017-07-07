package io.xol.chunkstories.world.io;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.workers.Task;
import io.xol.chunkstories.workers.TaskExecutor;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.concurrency.UniqueQueue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * This thread does I/O work in queue. Extended by IOTaskMultiplayerClient and IOTaskMultiplayerServer for the client/server model.
 */
public class IOTasks extends Thread implements TaskExecutor
{
	protected WorldImplementation world;

	protected UniqueQueue<IOTask> tasks = new UniqueQueue<IOTask>();
	protected Semaphore tasksCounter = new Semaphore(0);
	
	//private AtomicBoolean die = new AtomicBoolean();

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
		//TODO assert importance
		//if (die.get())
		//	return false;

		boolean code = tasks.add(task);
		if(code) {
			tasksCounter.release();
		}
		/*synchronized (this)
		{
			notifyAll();
		}*/
		return code;
	}

	@Override
	public String toString()
	{
		return "IO :" + getSize() + " in queue.";
	}

	@Override
	public void run()
	{
		System.out.println("IO Thread started.");

		this.setPriority(Constants.IO_THREAD_PRIOTITY);
		this.setName("IO Tasks");
		while (true)//(!die.get())
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
					ChunkStoriesLoggerImplementation.getInstance().warning("Exception occured when processing task : " + task);
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
		/*Runnable postRunOperation = null;

		public void setPostRunOperation(Runnable runnable)
		{
			postRunOperation = runnable;
		}*/

		//abstract public boolean run();

		@Override
		public void cancel()
		{
			super.cancel();
			
			//Kinf of redundant since it'll be skipped afterwards but meh
			tasks.remove(this);
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
			// If for some reasons the chunks holder's are still not loaded, we requeue the job
			if (!chunkSlot.getRegion().isDiskDataLoaded())
				return false;
			
			// When a loader was removed from the world, remaining operations on it are discarded
			if (chunkSlot.getRegion().isUnloaded())
				return true;
			
			// And so are redudant operations
			if (chunkSlot.isChunkLoaded())
				return true;

			RegionImplementation region = chunkSlot.getRegion();
			int cx = region.getRegionX() * 8 + chunkSlot.getInRegionX();
			int cy = region.getRegionY() * 8 + chunkSlot.getInRegionY();
			int cz = region.getRegionZ() * 8 + chunkSlot.getInRegionZ();

			//CubicChunk result;

			byte[] compressedData = chunkSlot.getCompressedData();
			if (compressedData == null || compressedData.length == 0)
			{
				chunkSlot.createChunk();
				//result = new CubicChunk(region, cx, cy, cz);
			}
			else
			{
				int data[] = new int[32 * 32 * 32];
				try
				{
					decompressor.decompress(compressedData, unCompressedDataBuffer.get());
				}
				catch (LZ4Exception e)
				{
					System.out.println("Failed to decompress chunk data (corrupted?) region:" + region + " chunk:" + cx + ":" + cy + ":" + cz + "task: " + this);
				}

				for (int i = 0; i < 32 * 32 * 32; i++)
				{
					data[i] = ((unCompressedDataBuffer.get()[i * 4] & 0xFF) << 24) | ((unCompressedDataBuffer.get()[i * 4 + 1] & 0xFF) << 16) | ((unCompressedDataBuffer.get()[i * 4 + 2] & 0xFF) << 8)
							| (unCompressedDataBuffer.get()[i * 4 + 3] & 0xFF);
				}
				
				chunkSlot.createChunk(data);
				//result = new CubicChunk(region, cx, cy, cz, data);
				
				//TODO Look into this properly
				//We never want to mess with that when we are the world
				//if(!(world instanceof WorldTool))
				//	result.bakeVoxelLightning(false);
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
					region.handler.load();
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
		RegionImplementation holder;

		public IOTaskSaveRegion(RegionImplementation holder)
		{
			this.holder = holder;
		}

		@Override
		public boolean task(TaskExecutor taskExecutor)
		{
			holder.handler.savingOperations.incrementAndGet();
			// First compress all loaded chunks !
			holder.compressAll();
			// Then write the file.
			try
			{
				holder.handler.save();
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
			//System.out.println("op b4"+this.holder.handler.savingOperations.get());
			this.holder.handler.savingOperations.decrementAndGet();
			//System.out.println("op aft"+this.holder.handler.savingOperations.get());
			synchronized (holder.handler)
			{
				holder.handler.notifyAll();
			}
			return true;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o != null && o instanceof IOTaskSaveRegion)
			{
				IOTaskSaveRegion comp = ((IOTaskSaveRegion) o);
				if (comp.holder.regionX == holder.regionX && comp.holder.regionY == this.holder.regionY && comp.holder.regionZ == this.holder.regionZ)
					return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return (666778 + 64 * holder.regionX + 22 * holder.regionY + 999 * holder.regionZ) % 2147483647;
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
	@Deprecated
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
	}

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
						ChunkStoriesLoggerImplementation.getInstance().error("Could not load load chunk summary at " + summary + " cause: " + e.getMessage());
					}

					summary.setData(heights, ids);
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

				summary.setData(heights, ids);
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
		//die.set(true);
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

	public void shutdown()
	{
		synchronized (this)
		{
			notifyAll();
		}
		while (this.tasks.size() > 0)
		{
			try
			{
				sleep(150L);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		scheduleTask(DIE);
		//die.set(true);
	}
}
