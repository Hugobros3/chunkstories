package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.world.ChunkHolder;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.summary.ChunkSummary;
import io.xol.engine.math.LoopingMathHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class IOTasks extends Thread
{

	public World world;
	AtomicBoolean die = new AtomicBoolean();

	// This thread does I/O work in queue.
	// Extended by IOTaskMultiplayerClient and IOTaskMultiplayerServer for the client/server model.

	protected Queue<IOTask> tasks = new ConcurrentLinkedQueue<IOTask>();
	int s = 0;
	int h = 0;

	public IOTasks(World world)
	{
		this.world = world;
		s = world.getSizeInChunks();
		h = world.getMaxHeight() / 32;
	}

	public void addTask(IOTask task)
	{
		//synchronized (tasks)
		{
			tasks.add(task);
		}
		synchronized (this)
		{
			notifyAll();
		}
		// System.out.println("Added task.");
	}

	public String toString()
	{
		//synchronized (tasks)
		{
			return "IOTasks : " + getSize() + " remaining.";
		}
	}

	public void requestChunksUnload(int pCX, int pCY, int pCZ, int sizeInChunks, int chunksViewDistance)
	{
		Iterator<IOTask> iter = tasks.iterator();
		while (iter.hasNext())
		{
			IOTask task = iter.next();
			if (task instanceof IOTaskLoadChunk)
			{
				IOTaskLoadChunk loadChunkTask = (IOTaskLoadChunk) task;
				int x = loadChunkTask.x;
				int y = loadChunkTask.y;
				int z = loadChunkTask.z;

				if ((LoopingMathHelper.moduloDistance(x, pCX, sizeInChunks) > chunksViewDistance) || (LoopingMathHelper.moduloDistance(y, pCZ, sizeInChunks) > chunksViewDistance) || (Math.abs(z - pCY) > 4))
				{
					//System.out.println("Removed task "+loadChunkTask+" for being too far");
					iter.remove();
				}
			}
		}
	}

	byte[] unCompressedData = new byte[32 * 32 * 32 * 4];
	LZ4Factory factory = LZ4Factory.fastestInstance();
	LZ4FastDecompressor decompressor = factory.fastDecompressor();

	public void run()
	{
		System.out.println("IO Thread started.");
		Thread.currentThread().setName("IO Tasks");
		while (!die.get())
		{
			//int count = 0;
			IOTask task = null;
			//synchronized (tasks)
			{
				//count = tasks.size();
				// Get first task in queue
				//if (count > 0)
					task = tasks.poll();
			}
			if (task == null)
			{
				//System.out.println("poll == null");
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
				}
			}
			else
			{
				// Run it
				try{
				boolean ok = task.run();
				// If it returns false, requeue it.
				//synchronized (tasks)
				{
					if (!ok)
					{
						//System.out.println("rescheduling");
						tasks.add(task);
					}
				}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		System.out.println("WorldLoader worker thread stopped");
	}

	public void kill()
	{
		die.set(true);
		synchronized (this)
		{
			notifyAll();
		}
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

	public abstract class IOTask
	{
		abstract public boolean run();
	}

	//Used for lightning
	Deque<Integer> blockSources = new ArrayDeque<Integer>();
	Deque<Integer> sunSources = new ArrayDeque<Integer>();
	
	public class IOTaskLoadChunk extends IOTask
	{
		public int x;
		public int y;
		public int z;
		boolean shouldLoadCH;
		boolean overwrite;

		public IOTaskLoadChunk(int x, int y, int z, boolean shouldLoadCH, boolean overwrite)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.shouldLoadCH = shouldLoadCH;
			this.overwrite = overwrite;
		}
		
		@Override
		public boolean run()
		{
			CubicChunk c = new CubicChunk(world, x, y, z);
			ChunkHolder holder = world.chunksHolder.getChunkHolder(x, y, z, shouldLoadCH);
			// If for some reasons the chunks holder's are still not loaded, we
			// requeue the job for later.
			if (holder == null)
				return false;
			if (!holder.isLoaded())
				return false;

			if (holder.isChunkLoaded(x, y, z) && !overwrite)
				return true;
			byte[] cd = holder.getCompressedData(x, y, z);
			if (cd == null)
			{
				// System.out.println("Null chunk :(");
			}
			else
			{
				decompressor.decompress(cd, unCompressedData);
				for (int i = 0; i < 32 * 32 * 32; i++)
				{
					int data = ((unCompressedData[i * 4] & 0xFF) << 24) | ((unCompressedData[i * 4 + 1] & 0xFF) << 16) | ((unCompressedData[i * 4 + 2] & 0xFF) << 8) | (unCompressedData[i * 4 + 3] & 0xFF);
					c.setDataAt(i / 32 / 32, (i / 32) % 32, i % 32, data);
				}
			}
			c.doLightning(false, blockSources, sunSources);
			world.setChunk(c);
			return true;
		}

	}

	public void requestChunkLoad(int chunkX, int chunkY, int chunkZ, boolean overwrite)
	{
		IOTaskLoadChunk task = new IOTaskLoadChunk(chunkX, chunkY, chunkZ, true, overwrite);
		//System.out.println("req CL "+chunkX+":"+chunkY+":"+chunkZ);
		
		// We now do duplicate check in ChunkHolder

		for (IOTask ioTask : tasks)
		{
			if (ioTask instanceof IOTaskLoadChunk)
			{
				IOTaskLoadChunk taskLC = (IOTaskLoadChunk) ioTask;
				if (taskLC.x == task.x && taskLC.y == task.y && taskLC.z == task.z)
					return;
			}
		}
		addTask(task);
	}

	public class IOTaskLoadChunkHolder extends IOTask
	{
		ChunkHolder holder;

		public IOTaskLoadChunkHolder(ChunkHolder holder)
		{
			this.holder = holder;
		}

		@Override
		public boolean run()
		{
			if (holder.handler.exists())
			{
				// System.out.println("Loading existing chunk holder...");
				try
				{
					FileInputStream in = new FileInputStream(holder.handler);
					int[] chunksSizes = new int[8 * 8 * 8];
					// First load the index
					for (int a = 0; a < 8 * 8 * 8; a++)
					{
						int size = in.read() << 24;
						size += in.read() << 16;
						size += in.read() << 8;
						size += in.read();
						chunksSizes[a] = size;
					}
					// Then load the chunks
					// int i = 0;
					for (int a = 0; a < 8; a++)
						for (int b = 0; b < 8; b++)
							for (int c = 0; c < 8; c++)
							{
								int size = chunksSizes[a * 8 * 8 + b * 8 + c];
								// if chunk present then create it's byte array
								// and
								// fill it
								if (size > 0)
								{
									holder.compressedChunks[a][b][c] = new byte[size];
									in.read(holder.compressedChunks[a][b][c], 0, size);
									// i++;
								}
							}
					// System.out.println("read "+i+" compressed chunks");
					in.close();
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				//Generate this crap !
				holder.generateAll();
			}
			holder.setLoaded(true);
			return true;
		}

	}

	public void requestChunkHolderLoad(ChunkHolder holder)
	{
		IOTask task = new IOTaskLoadChunkHolder(holder);
		addTask(task);
	}

	public class IOTaskSaveChunkHolder extends IOTask
	{
		ChunkHolder holder;

		public IOTaskSaveChunkHolder(ChunkHolder holder)
		{
			this.holder = holder;
		}

		@Override
		public boolean run()
		{
			// First compress all loaded chunks !
			ChunksIterator i = holder.iterator();
			CubicChunk cu;
			while(i.hasNext())
			{
				cu = i.next();
				holder.compressChunkData(cu);
			}
			//for (CubicChunk c : holder.getLoadedChunks())
			//	holder.compressChunkData(c);
			// Then write the file.
			try
			{
				holder.handler.getParentFile().mkdirs();
				if (!holder.handler.exists())
					holder.handler.createNewFile();
				FileOutputStream out = new FileOutputStream(holder.handler);
				// int[] chunksSizes = new int[8*8*8];
				// First write the index
				for (int a = 0; a < 8; a++)
					for (int b = 0; b < 8; b++)
						for (int c = 0; c < 8; c++)
						{
							int chunkSize = 0;
							if (holder.compressedChunks[a][b][c] != null)
							{
								chunkSize = holder.compressedChunks[a][b][c].length;
							}
							out.write((chunkSize >>> 24) & 0xFF);
							out.write((chunkSize >>> 16) & 0xFF);
							out.write((chunkSize >>> 8) & 0xFF);
							out.write((chunkSize >>> 0) & 0xFF);
						}
				// Then write said chunks
				for (int a = 0; a < 8; a++)
					for (int b = 0; b < 8; b++)
						for (int c = 0; c < 8; c++)
						{
							if (holder.compressedChunks[a][b][c] != null)
							{
								out.write(holder.compressedChunks[a][b][c]);
							}
						}

				out.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return true;
		}

	}

	public void requestChunkHolderSave(ChunkHolder holder)
	{
		IOTask task = new IOTaskSaveChunkHolder(holder);
		addTask(task);
	}

	public class IOTaskLoadSummary extends IOTask
	{
		ChunkSummary summary;

		public IOTaskLoadSummary(ChunkSummary summary)
		{
			this.summary = summary;
		}

		@Override
		public boolean run()
		{
			if (summary.handler.exists())
			{
				try
				{
					FileInputStream in = new FileInputStream(summary.handler);

					byte[] size = new byte[4];
					in.read(size);
					int s = ByteBuffer.wrap(size).asIntBuffer().get(0);
					byte[] compressed = new byte[s];
					in.read(compressed);

					byte[] decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
					IntBuffer ib = ByteBuffer.wrap(decompressed).asIntBuffer();
					for (int i = 0; i < 256 * 256; i++)
						summary.heights[i] = ib.get();

					in.read(size);
					s = ByteBuffer.wrap(size).asIntBuffer().get(0);
					compressed = new byte[s];
					in.read(compressed);

					decompressed = decompressor.decompress(compressed, 256 * 256 * 4);
					ib = ByteBuffer.wrap(decompressed).asIntBuffer();
					for (int i = 0; i < 256 * 256; i++)
						summary.ids[i] = ib.get();

					in.close();

					summary.uploadUpToDate.set(false);
					summary.loaded.set(true);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println("Couldn't find file : " + summary.handler.getAbsolutePath());
				// Generate summary according to generator heightmap
				int h, t;
				for (int x = 0; x < 256; x++)
					for (int z = 0; z < 256; z++)
					{
						h = world.accessor.getHeightAt(x + summary.rx * 256, z + summary.rz * 256);
						t = world.accessor.getDataAt(x + summary.rx * 256, h, z + summary.rz * 256, h);
						summary.heights[x * 256 + z] = h;
						summary.ids[x * 256 + z] = t;
					}

				summary.loaded.set(true);
				// then save
				summary.save(summary.handler);
				// Thread.dumpStack();
			}
			return true;
		}

	}

	public void requestChunkSummaryLoad(ChunkSummary summary)
	{
		IOTask task = new IOTaskLoadSummary(summary);
		addTask(task);
	}
	
	public class IOTaskSaveSummary extends IOTask
	{

		ChunkSummary summary;

		public IOTaskSaveSummary(ChunkSummary summary)
		{
			this.summary = summary;
		}

		@Override
		public boolean run()
		{
			try
			{
				summary.handler.getParentFile().mkdirs();
				if (!summary.handler.exists())
					summary.handler.createNewFile();
				FileOutputStream out = new FileOutputStream(summary.handler);

				ByteBuffer writeMe = ByteBuffer.allocate(256 * 256 * 4);
				for (int i : summary.heights)
					writeMe.putInt(i);

				byte[] compressed = ChunkSummary.compressor.compress(writeMe.array());

				int compressedSize = compressed.length;

				byte[] size = ByteBuffer.allocate(4).putInt(compressedSize).array();
				out.write(size);
				out.write(compressed);

				writeMe.clear();
				for (int i : summary.ids)
					writeMe.putInt(i);

				compressed = ChunkSummary.compressor.compress(writeMe.array());
				compressedSize = compressed.length;

				size = ByteBuffer.allocate(4).putInt(compressedSize).array();
				out.write(size);
				out.write(compressed);

				out.close();
				// System.out.println("saved");
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}

	}

	public void requestChunkSummarySave(ChunkSummary summary)
	{
		IOTask task = new IOTaskSaveSummary(summary);
		addTask(task);
	}

	public void notifyChunkUnload(int chunkX, int chunkY, int chunkZ)
	{
		
	}
	
	public class IOTaskRunWithHolder extends IOTask
	{
		ChunkHolder holder;
		IORequiringTask task;

		public IOTaskRunWithHolder(ChunkHolder holder, IORequiringTask task)
		{
			this.holder = holder;
			this.task = task;
		}

		@Override
		public boolean run()
		{
			if(!holder.isLoaded())
			{
				System.out.println("Trying to load chunk holder for requiring task");
				world.chunksHolder.getChunkHolder(holder.regionX * 8, holder.regionY * 8, holder.regionZ * 8, true);
				return false;
			}
			else
			{
				return task.run(holder);
			}
		}

	}
	
	public void requestChunkHolderRequest(ChunkHolder holder, IORequiringTask job)
	{
		IOTaskRunWithHolder task = new IOTaskRunWithHolder(holder, job);
		addTask(task);
	}
}
